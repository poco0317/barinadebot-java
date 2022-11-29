package bar.barinade.barry.discord.serverconfig.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import bar.barinade.barry.discord.serverconfig.data.TwitchGuildAssociation;

@Repository
public interface TwitchGuildAssociationRepo extends JpaRepository<TwitchGuildAssociation, Long> {

	List<TwitchGuildAssociation> findByGuildId(Long id);
	Long deleteByGuildId(Long id);
	Long deleteByTwitchId(Long id);
	
}
