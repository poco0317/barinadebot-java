package bar.barinade.barry.discord.serverconfig.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import bar.barinade.barry.discord.serverconfig.data.ServerConfiguration;

@Repository
public interface ServerConfigurationRepo extends JpaRepository<ServerConfiguration, Long> {

}
