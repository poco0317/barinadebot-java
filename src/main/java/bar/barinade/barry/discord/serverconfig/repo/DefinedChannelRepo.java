package bar.barinade.barry.discord.serverconfig.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import bar.barinade.barry.discord.serverconfig.data.DefinedChannel;
import bar.barinade.barry.discord.serverconfig.data.pk.DefinedChannelId;

@Repository
public interface DefinedChannelRepo extends JpaRepository<DefinedChannel, DefinedChannelId> {
	
	List<DefinedChannel> findByIdGuildId(Long id);
	Long deleteByIdGuildId(Long id);

}
