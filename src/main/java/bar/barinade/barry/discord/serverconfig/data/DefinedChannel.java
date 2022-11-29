package bar.barinade.barry.discord.serverconfig.data;

import java.util.Objects;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

import bar.barinade.barry.discord.serverconfig.data.pk.DefinedChannelId;

@Entity
@Table(name = "defined_channels")
public class DefinedChannel {

	@EmbeddedId
	private DefinedChannelId id;

	public DefinedChannelId getId() {
		return id;
	}

	public void setId(DefinedChannelId id) {
		this.id = id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DefinedChannel other = (DefinedChannel) obj;
		return Objects.equals(id, other.id);
	}
	
}
