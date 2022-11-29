package bar.barinade.barry.discord.serverconfig.data;


import java.util.Objects;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "server_configs")
public class ServerConfiguration {
	
	@Id
	@Column(name = "guild_id")
	private Long id;
	
	@OneToMany(mappedBy = "id.guild")
	private Set<DefinedChannel> definedChannels;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Set<DefinedChannel> getDefinedChannels() {
		return definedChannels;
	}

	public void setDefinedChannels(Set<DefinedChannel> definedChannels) {
		this.definedChannels = definedChannels;
	}

	@Override
	public int hashCode() {
		return Objects.hash(definedChannels, id);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ServerConfiguration other = (ServerConfiguration) obj;
		return Objects.equals(definedChannels, other.definedChannels) && Objects.equals(id, other.id);
	}

}
