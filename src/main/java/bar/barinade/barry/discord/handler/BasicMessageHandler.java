package bar.barinade.barry.discord.handler;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import bar.barinade.barry.brain.service.Cerebellum;
import bar.barinade.barry.discord.BotPermissions;
import bar.barinade.barry.discord.serverconfig.service.DefinedChannelService;
import bar.barinade.barry.twitch.TwitchChatManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.MentionType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

@Component
@Scope("prototype")
public class BasicMessageHandler extends ListenerAdapter {
	
	private static final Logger m_logger = LoggerFactory.getLogger(BasicMessageHandler.class);
	
	private static final String PREFIX = "br!";
	private static final String NAME_LOAD_PATH = "br!loadpath";
	private static final String LISTEN = "br!listen";
	private static final String ASSIGN_TWITCH = "br!twitch";
	private static final String UNASSIGN_TWITCH = "br!stoptwitch";
	private static final String SERVERS = "br!servers";
	
	private static final String BAR_PREFIX = "bar";
	
	@Autowired
	private Cerebellum brain;
	
	@Autowired
	private DefinedChannelService channels;
	
	@Autowired
	private BotPermissions perms;
	
	@Autowired
	private TwitchChatManager twitchChat;
	
	@Value("${discord.ownerid}")
	private String ownerId;
	
	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		final String msg = event.getMessage().getContentDisplay();
		if (msg == null) {
			return;
		}
		
		try {
			if (event.getGuild() == null) {
				return;
			}
			if (event.getChannel() == null) {
				return;
			}
			if (event.getAuthor().isBot()) {
				return;
			}
		} catch (Exception e) {}
		
		
		final Long id = event.getGuild().getIdLong();
		final Long chanId = event.getChannel().getIdLong();
		
		// bot host commands
		if (event.getAuthor().getId().equals(ownerId)) {
			if (msg.startsWith(PREFIX)) {
				m_logger.info("About to attempt cmd: {}", msg);
				
				// load brain from file path
				if (msg.startsWith(NAME_LOAD_PATH)) {					
					String arg = msg.substring(NAME_LOAD_PATH.length()).strip();
					
					brain.loadFormattedBrainFromPath(arg, id);
				}
				else if (msg.startsWith(ASSIGN_TWITCH)) {
					String arg = msg.substring(ASSIGN_TWITCH.length()).strip();
					
					boolean success = twitchChat.setAssociation(id, arg);
					if (!success) {
						event.getChannel().sendMessage(
								"Failed to associate Twitch channel with this guild. The channel may already be in use, or the channel did not exist.").queue();
					} else {
						event.getChannel().sendMessage("Successfully added Twitch channel to associate with this guild.").queue();
					}
				}
				else if (msg.startsWith(UNASSIGN_TWITCH)) {
					boolean success = twitchChat.removeAssociation(id);
					if (!success) {
						event.getChannel().sendMessage(
								"Failed to remove Twitch channel association with this guild ... There was an error or there was never an association.").queue();
					} else {
						event.getChannel().sendMessage("Successfully removed Twitch channel association from this guild.").queue();
					}
				}
				else if (msg.startsWith(SERVERS)) {
					List<Guild> guilds = event.getJDA().getGuilds();
					StringBuilder sb = new StringBuilder();
					for (Guild guild : guilds) {
						sb.append(guild.getId() + " - "+guild.getName());
					}
					event.getChannel().sendMessage("Heres the servers: "+sb.toString()).queue();
				}
				return;
			}
		}
		
		// management commands
		// toggle listening on a channel (off by default)
		if (msg.startsWith(LISTEN) && perms.hasPermission(event, true)) {
			if (!channels.add(id, chanId)) {
				channels.remove(id, chanId);
				event.getChannel().sendMessage("Removed this channel from listen list").queue();
			} else {
				event.getChannel().sendMessage("Added this channel to listen list").queue();
			}
			return;
		}
		
		// bar invocations
		if (triggersResponse(event.getMessage()) && channels.channelEnabled(id, chanId)) {
			String args = msg.substring(BAR_PREFIX.length()).toLowerCase().strip();
			m_logger.info("GUILD {} || Barry is responding to '{}'", id, args);
			String r = brain.formSentence(id, args);
			r = r.replaceAll("@", "");
			m_logger.info("GUILD {} || Barry replied '{}'", id, r);
			event.getChannel().sendMessage(r).queue();
			return;
		}
		
		
		// if it makes it this far ... try to parse the message
		try {
			if (channels.channelEnabled(id, chanId)) {
				brain.parseAndCommitSentence(id, event.getMessage().getContentStripped(), false);
			}
		} catch (Exception e) {
			String fm = String.format("Error parsing message - GUILD %d - MESSAGE ||| %s --||-- %s", id, event.getMessage().getContentStripped(), e.getMessage());
			m_logger.error(fm, e);
		}
	}
	
	/**
	 * True if:
	 * - "bar ..."
	 * - mentions this bot
	 */
	private boolean triggersResponse(Message msg) {
		final String content = msg.getContentStripped();
		return content.toLowerCase().startsWith(BAR_PREFIX) || msg.isMentioned(msg.getJDA().getSelfUser(), MentionType.USER);
	}
}
