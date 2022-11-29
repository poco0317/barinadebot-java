package bar.barinade.barry.discord.serverconfig.data;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "twitch_guilds")
public class TwitchGuildAssociation {
	
	@Id
	@Column(name = "twitch_id")
	private Long twitchId;
	
	@Column(name = "guild_id")
	private Long guildId;

	public Long getTwitchId() {
		return twitchId;
	}

	public void setTwitchId(Long twitchId) {
		this.twitchId = twitchId;
	}

	public Long getGuildId() {
		return guildId;
	}

	public void setGuildId(Long guildId) {
		this.guildId = guildId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(guildId, twitchId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TwitchGuildAssociation other = (TwitchGuildAssociation) obj;
		return Objects.equals(guildId, other.guildId) && Objects.equals(twitchId, other.twitchId);
	}

}
