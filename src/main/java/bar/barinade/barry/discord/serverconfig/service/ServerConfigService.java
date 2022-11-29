package bar.barinade.barry.discord.serverconfig.service;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import bar.barinade.barry.discord.serverconfig.data.ServerConfiguration;
import bar.barinade.barry.discord.serverconfig.repo.ServerConfigurationRepo;

@Service
public class ServerConfigService {

	@Autowired
	private ServerConfigurationRepo configRepo;
	
	@Transactional
	public ServerConfiguration getConfig(Long guildId) {
		ServerConfiguration config = configRepo.findById(guildId).orElse(null);
		if (config == null) {
			config = new ServerConfiguration();
			config.setId(guildId);
			config = configRepo.saveAndFlush(config);
		}
		return config;
	}
}
