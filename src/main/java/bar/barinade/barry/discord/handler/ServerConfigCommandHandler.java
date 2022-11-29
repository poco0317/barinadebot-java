package bar.barinade.barry.discord.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import bar.barinade.barry.brain.service.Cerebellum;
import bar.barinade.barry.discord.BotPermissions;
import bar.barinade.barry.discord.MessageHistoryService;
import bar.barinade.barry.discord.serverconfig.service.DefinedChannelService;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

@Component
@Scope("prototype")
public class ServerConfigCommandHandler extends CommandHandlerBase {
	
	private static final Logger m_logger = LoggerFactory.getLogger(ServerConfigCommandHandler.class);
	
	private static final String MAINCMD_NAME_SUB = "listen";
	private static final String MAINCMD_NAME_LEARN = "learn";
	private static final String OPTION_CHANNEL = "channel";
	
	@Autowired
	private DefinedChannelService channelService;
	
	@Autowired
	private BotPermissions perms;
	
	@Autowired
	private MessageHistoryService history;
	
	@Autowired
	private Cerebellum brain;
	
	@Override
	public CommandData[] getCommandsToUpsert() {
		return new CommandData[] {
				new CommandData(MAINCMD_NAME_SUB, "Toggle Barry listening on a particular channel")
				.addOption(OptionType.CHANNEL, OPTION_CHANNEL, "Text channel to toggle listening", true),
				new CommandData(MAINCMD_NAME_LEARN, "Have Barry read all history of a channel to learn how to talk (very slow!)")
				.addOption(OptionType.CHANNEL, OPTION_CHANNEL, "Text channel to target", true),
		};
	}
	
	void cmd_listen(SlashCommandEvent event) {
		if (!perms.hasPermission(event, true)) {
			return;
		}
		handleSetChannel(event);
	}
	
	void cmd_learn(SlashCommandEvent event) {
		if (!perms.hasPermission(event, true)) {
			return;
		}
		handleLearn(event);
	}
	
	private void handleSetChannel(SlashCommandEvent event) {
		final Long id = event.getGuild().getIdLong();
		final ChannelType chantype = event.getOption(OPTION_CHANNEL).getChannelType();
		if (!chantype.equals(ChannelType.TEXT)) {
			m_logger.info("User syntax error: Given a Channel which was not a Text Channel.");
			event.getHook().editOriginal("You must specify a Text Channel. Your channel was of type '"+chantype.toString()+"'").queue();
			return;
		}
		final MessageChannel channel = event.getOption(OPTION_CHANNEL).getAsMessageChannel();
		if (!channelService.add(id, channel.getIdLong())) {
			channelService.remove(id, channel.getIdLong());
			event.getHook().editOriginal("Removed channel '"+channel.getName()+"' from listen list").queue();
		} else {
			event.getHook().editOriginal("Added channel '"+channel.getName()+"' to listen list").queue();
		}
	}
	
	private void handleLearn(SlashCommandEvent event) {
		final Long id = event.getGuild().getIdLong();
		final ChannelType chantype = event.getOption(OPTION_CHANNEL).getChannelType();
		if (!chantype.equals(ChannelType.TEXT)) {
			m_logger.debug("User syntax error: Given a Channel which was not a Text Channel.");
			event.getHook().editOriginal("You must specify a Text Channel. Your channel was of type '"+chantype.toString()+"'").queue();
			return;
		}
		final MessageChannel channel = event.getOption(OPTION_CHANNEL).getAsMessageChannel();
		final Long chanId = channel.getIdLong();
		boolean state = history.isInProgress(chanId);
		if (state) {
			// if true, not allowed to start it again until it is finished
			m_logger.info("User error: Tried to start channel learn while channel already in progress. GUILD {} | CHAN {}", id, chanId);
			event.getHook().editOriginal("The specified channel ("+channel.getName()+") has a learn in progress already. Please wait for it to finish. You may start a different channel.").queue();
		} else {
			event.getHook().editOriginal("Learning from channel '"+channel.getName()+"' ... this may take a long time. Will send a message when done.").queue();
			history.cacheAllChannelHistory(
				channel,
				allMessages -> {
					event.getChannel().sendMessage("Message history learning finished for specified channel ("+channel.getName()+"). Read through "+allMessages.size()+" messages.").queue();
				},
				messageList -> {
					messageList.forEach(m -> {
						try {
							if (!m.getAuthor().isBot()) {
								String sentence = m.getContentStripped();
								// true defers flush so we can move a little faster
								brain.parseAndCommitSentence(id, sentence, true);
							}
						} catch (Exception e) {
							// not worth it
						}
					});
					brain.flushNeurons();
					
					long sz = history.getCacheSize(channel);
					m_logger.info("In progress channel {} ... total so far {}", channel.getName(), sz);
					// event.getHook().editOriginal("In progress channel '"+channel.getName()+"' ... message cache total: "+sz).queue();
				});
		}
	}
	

}
