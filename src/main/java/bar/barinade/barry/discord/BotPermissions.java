package bar.barinade.barry.discord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

@Service
public class BotPermissions {
	
	private static final Logger m_logger = LoggerFactory.getLogger(BotPermissions.class);
	
	@Value("${discord.ownerid}")
	private String ownerId;
	
	public String getBotHostId() {
		return ownerId;
	}
	
	public boolean isBotHost(Member mmbr) {
		return mmbr != null && mmbr.getId().equals(ownerId);
	}
	
	public boolean isServerOwner(Member mmbr) {
		return mmbr != null && mmbr.isOwner();
	}
	
	public boolean isServerAdmin(Member mmbr) {
		return mmbr != null && mmbr.hasPermission(Permission.ADMINISTRATOR);
	}
	
	public boolean isServerManager(Member mmbr) {
		return mmbr != null && mmbr.hasPermission(Permission.MANAGE_SERVER);
	}
	
	/**
	 * Does the user have Manage Server, Administrator, Owner, or Bot Host permissions?
	 */
	public boolean hasPermission(GenericEvent ievent, boolean replyIfFailure) {
		Member mmbr = null;
		
		if (ievent instanceof MessageReceivedEvent) {
			mmbr = ((MessageReceivedEvent)ievent).getMember();
		} else if (ievent instanceof SlashCommandEvent) {
			mmbr = ((SlashCommandEvent)ievent).getMember();
		}
		
		if (!isBotHost(mmbr) && !isServerOwner(mmbr) && !isServerAdmin(mmbr) && !isServerManager(mmbr)) {
			m_logger.info("{} attempted to use command without having permission", mmbr.getId());
			
			if (replyIfFailure) {
				if (ievent instanceof MessageReceivedEvent) {
					((MessageReceivedEvent)ievent).getChannel().sendMessage("You must have Manage Server or Administrator permissions to use this command.").queue();
				} else if (ievent instanceof SlashCommandEvent) {
					((SlashCommandEvent)ievent).getHook().editOriginal("You must have Manage Server or Administrator permissions to use this command.").queue();	
				}
			}
			
			return false;
		}
		return true;
	}

}
