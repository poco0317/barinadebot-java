package bar.barinade.barry.discord.serverconfig.data.pk;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import bar.barinade.barry.discord.serverconfig.data.ServerConfiguration;

@Embeddable
public class DefinedChannelId implements Serializable {
	private static final long serialVersionUID = 1L;
	
	@Column(name = "channel_id", nullable = false)
	private Long channelId;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "guild_id", nullable = false)
	private ServerConfiguration guild;

	public DefinedChannelId() {}
	
	public DefinedChannelId(Long channel, ServerConfiguration guild) {
		this.channelId = channel;
		this.guild = guild;
	}

	public Long getChannelId() {
		return channelId;
	}

	public void setChannelId(Long channelId) {
		this.channelId = channelId;
	}

	public ServerConfiguration getGuild() {
		return guild;
	}

	public void setGuild(ServerConfiguration guild) {
		this.guild = guild;
	}

	@Override
	public int hashCode() {
		return Objects.hash(channelId, guild);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DefinedChannelId other = (DefinedChannelId) obj;
		return Objects.equals(channelId, other.channelId) && Objects.equals(guild, other.guild);
	}
	
	
	
}
