package bar.barinade.barry.discord.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import bar.barinade.barry.twitch.TwitchChatManager;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

@Component
@Scope("prototype")
public class GuildLifecycleHandler extends ListenerAdapter {
	
	private static final Logger m_logger = LoggerFactory.getLogger(GuildLifecycleHandler.class);
	
	@Autowired
	private TwitchChatManager twitchChatManager;
	
	@Override
	public void onGuildJoin(GuildJoinEvent event) {
		m_logger.info("Joined new Guild : ID {} : {}", event.getGuild().getId(), event.getGuild().getName());
	}
	
	@Override
	public void onGuildLeave(GuildLeaveEvent event) {
		m_logger.info("Left Guild : ID {} : {}", event.getGuild().getId(), event.getGuild().getName());
		twitchChatManager.removeAssociation(event.getGuild().getIdLong());
	}

}
