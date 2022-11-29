package bar.barinade.barry.twitch;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.github.philippheuer.credentialmanager.CredentialManager;
import com.github.philippheuer.credentialmanager.CredentialManagerBuilder;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.auth.providers.TwitchIdentityProvider;
import com.github.twitch4j.chat.TwitchChat;
import com.github.twitch4j.chat.TwitchChatBuilder;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.common.events.domain.EventChannel;
import com.github.twitch4j.helix.TwitchHelix;
import com.github.twitch4j.helix.TwitchHelixBuilder;
import com.github.twitch4j.helix.domain.User;
import com.github.twitch4j.helix.domain.UserList;

import bar.barinade.barry.brain.service.Cerebellum;
import bar.barinade.barry.discord.serverconfig.data.TwitchGuildAssociation;
import bar.barinade.barry.discord.serverconfig.service.TwitchGuildAssociationService;

@Service
public class TwitchChatManager {
	
	private static final Logger m_logger = LoggerFactory.getLogger(TwitchChatManager.class);
	
	private CredentialManager credman;
	private TwitchChat chat;
	private TwitchHelix api;
	private TwitchChannelIdCache channelCache = new TwitchChannelIdCache();
	
	@Value("${streams.twitch.clientid}")
	private String clientId;
	
	@Value("${streams.twitch.clientsecret}")
	private String clientSecret;
	
	@Value("${streams.twitch.oauth}")
	private String oauth;
	
	@Value("${streams.twitch.username}")
	private String SELF_NAME;
	
	@Autowired
	private Cerebellum speech;
	
	@Autowired
	private TwitchGuildAssociationService channelToGuild;
		
	@PostConstruct
	private void init() {
		m_logger.info("Initializing TwitchChatManager");
		
		credman = CredentialManagerBuilder.builder().build();
		credman.registerIdentityProvider(new TwitchIdentityProvider(clientId, clientSecret, ""));
		
		chat = TwitchChatBuilder.builder()
				.withCredentialManager(credman)
				.withClientId(clientId)
				.withClientSecret(clientSecret)
				.withChatAccount(new OAuth2Credential("twitch", oauth))
				.build();
		
		api = TwitchHelixBuilder.builder()
				.withClientId(clientId)
				.withClientSecret(clientSecret)
				.withUserAgent("BarinadeBot/0.0.1")
				.build();
		
		/*
		// raw IRC messages
		chat.getEventManager().onEvent(IRCMessageEvent.class, e -> {
			m_logger.trace(e.getRawMessage());
		});
		*/
		
		chat.getEventManager().onEvent(ChannelMessageEvent.class, e -> {
			handleChatEvent(e);
		});
		
		chat.connect();
		refreshJoinedChannels();
		
		m_logger.info("Finished initializing TwitchChatManager");
	}

	private void handleChatEvent(ChannelMessageEvent event) {
		EventChannel channel =  event.getChannel();
		if (channel != null) {
			String msg = event.getMessage();
			if (msg != null && !msg.isEmpty()) {
				if (msg.startsWith("@" + SELF_NAME)) {
					Long channelId = resolveIdByName(channel.getName());
					if (channelId != null) {
						Long brainId = getBrainFromChannelId(channelId);
						if (brainId != null) {
							String args = msg.substring(1 + SELF_NAME.length()).toLowerCase().strip();
							m_logger.info("TWITCH {} || Barry is responding to '{}'", channel.getName(), args);
							String r = speech.formSentence(brainId, args);
							chat.sendMessage(channel.getName(), r);
							m_logger.info("TWITCH {} || Barry replied '{}'", channel.getName(), r);
						}
					}
				} else {
					Long channelId = resolveIdByName(channel.getName());
					if (channelId != null) {
						Long brainId = getBrainFromChannelId(channelId);
						if (brainId != null) {
							try {
								speech.parseAndCommitSentence(brainId, msg, false);
							} catch (Exception ex) {
								m_logger.error("Error parsing chat msg ("+ex.getMessage()+") - MESSAGE: "+msg, ex);
							}
						}
					}
				}
			}
		}
	}
	
	public boolean setAssociation(Long guildId, String channelName) {
		Long twitchId = resolveIdByName(channelName);
		if (twitchId == null) {
			return false;
		}
		boolean result = channelToGuild.setTwitchIdForGuild(guildId, twitchId);
		if (result) {
			refreshJoinedChannels();
		}
		return result;
	}
	
	public boolean removeAssociation(Long guildId) {
		boolean result = channelToGuild.deleteByGuildId(guildId) > 0L;
		if (result) {
			refreshJoinedChannels();
		}
		return result;
	}
	
	public Long getBrainFromChannelId(Long channelId) {
		TwitchGuildAssociation association = channelToGuild.getByTwitchId(channelId);
		if (association != null) {
			return association.getGuildId();
		}
		return null;
	}
	
	public void refreshJoinedChannels() {
		if (chat == null ) {
			m_logger.error("Attempted to refresh Twitch Chats when there was no Chat API ...");
		} else {
			m_logger.info("Refreshing joined Twitch Chat list...");
			
			List<TwitchGuildAssociation> channelsShouldBeJoined = channelToGuild.getAll();
			if (channelsShouldBeJoined == null || channelsShouldBeJoined.size() == 0) {
				chat.getChannels().forEach(name -> chat.leaveChannel(name));
				m_logger.info("Left all Twitch Chats since list was empty");
			} else {
				Set<String> channelNamesShouldBeJoined = channelsShouldBeJoined.stream().map(e -> resolveNameById(e.getTwitchId())).collect(Collectors.toSet());
				Set<String> channelsToJoin = channelNamesShouldBeJoined.stream().filter(e -> !chat.getChannels().contains(e)).collect(Collectors.toSet());
				channelsToJoin.forEach(name -> chat.joinChannel(name));
				Set<String> channelsToLeave = chat.getChannels().stream().filter(e -> !channelNamesShouldBeJoined.contains(e)).collect(Collectors.toSet());
				channelsToLeave.forEach(name -> chat.leaveChannel(name));
				m_logger.info("Joined {} Twitch Chats || Left {} Twitch Chats || {} total", channelsToJoin.size(), channelsToLeave.size(), chat.getChannels().size());
			}
			
			m_logger.info("Finished refreshing joined Twitch Chat list");
		}
	}
	
	private String resolveNameById(Long channelId) {
		String channelName = channelCache.get(channelId);
		if (channelName != null) {
			return channelName;
		}
		
		List<String> id = Arrays.asList(new String[] {channelId.toString()});
		UserList userList = api.getUsers(null, id, null).execute();
		if (userList.getUsers().size() < 1) {
			return null;
		}
		
		User user = userList.getUsers().get(0);
		channelName = user.getLogin();
		channelCache.put(channelName, channelId.toString());
		m_logger.info("Cached new Twitch Channel to ID entry : {} - {}", channelName, channelId);
		
		return channelName;
	}
	
	private Long resolveIdByName(String channelLoginName) {
		String channelId = channelCache.get(channelLoginName);
		if (channelId != null) {
			return Long.parseLong(channelId);
		}
		
		List<String> name = Arrays.asList(new String[] {channelLoginName});
		UserList userList = api.getUsers(null, null, name).execute();
		if (userList.getUsers().size() < 1) {
			return null;
		}
		
		User user = userList.getUsers().get(0);
		channelId = user.getId();
		channelCache.put(channelLoginName, channelId);
		m_logger.info("Cached new Twitch Channel to ID entry : {} - {}", channelLoginName, channelId);
		
		return Long.parseLong(channelId);
	}

}
