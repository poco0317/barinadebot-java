package bar.barinade.barry.discord.handler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;


public abstract class CommandHandlerBase extends ListenerAdapter {
	
	private static final Logger m_logger = LoggerFactory.getLogger(CommandHandlerBase.class);

	protected HashSet<String> commandNames;
	protected HashMap<String, Method> commandEventHandlers;
	
	private final String CMD_EVENT_HANDLER_PREFIX = "cmd_";
	
	@PostConstruct
	private void init() {
		commandNames = new HashSet<>();
		for (final CommandData cd : getCommandsToUpsert()) {
			commandNames.add(cd.getName());
		}
		
		commandEventHandlers = new HashMap<>();
		for (final Method m : this.getClass().getDeclaredMethods()) {
			if (m.getName().startsWith(CMD_EVENT_HANDLER_PREFIX)) {
				final String name = m.getName().substring(CMD_EVENT_HANDLER_PREFIX.length());
				m.setAccessible(true);
				commandEventHandlers.put(name, m);
			}
		}
	}
	
	/**
	 * Defines the list of slash commands this handler listens for and needs to register.
	 * If this returns null we cant launch
	 */
	public abstract CommandData[] getCommandsToUpsert();
	
	@Override
	public void onSlashCommand(SlashCommandEvent event) {
		
		final String name = event.getName();
		final String group = event.getSubcommandGroup();
		final String subname = event.getSubcommandName();
		final Method handler = commandEventHandlers.getOrDefault(name, null);
		
		if (event.getUser().isBot()) {
			return;
		}
		
		if (!event.isFromGuild()) {
			m_logger.info("{} attempted to use {} outside of a guild.", event.getUser().getId(), name);
			event.reply("Cannot invoke commands from DM").setEphemeral(true).queue();
			return;
		}
		
		if (handler == null) {
			m_logger.warn("Received command event for unknown handler! Command name: {}", name);
		} else {
			
			if (group != null && subname != null) {
				m_logger.trace("{} invoking Command: {} | GROUP {} | SUB {}", event.getUser().getId(), name, group, subname);
			} else if (group != null) {
				m_logger.trace("{} invoking Command: {} | GROUP {}", event.getUser().getId(), name, group);
			} else {
				m_logger.trace("{} invoking Command: {}", event.getUser().getId(), name);
			}
			
			try {
				event.deferReply().queue();
				handler.invoke(this, event);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				m_logger.error("Error in method invocation: "+e.getMessage(), e);
				event.getHook().editOriginal("Internal error occurred when processing command: "+e.getClass().getSimpleName()).queue();
			} catch (RuntimeException e) {
				m_logger.error("Error in method invocation: "+e.getMessage(), e);
				event.getHook().editOriginal("Internal error occurred when processing command: "+e.getClass().getSimpleName()).queue();
			}
		}
	}
	
}
