package bar.barinade.barry.discord.serverconfig.service;

import java.util.List;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import bar.barinade.barry.discord.serverconfig.data.DefinedChannel;
import bar.barinade.barry.discord.serverconfig.data.ServerConfiguration;
import bar.barinade.barry.discord.serverconfig.data.pk.DefinedChannelId;
import bar.barinade.barry.discord.serverconfig.repo.DefinedChannelRepo;

@Service
public class DefinedChannelService {
	
	private static final Logger m_logger = LoggerFactory.getLogger(DefinedChannelService.class);
	
	@Autowired
	private ServerConfigService configService;
	
	@Autowired
	private DefinedChannelRepo channelRepo;
	
	@Transactional
	public DefinedChannel get(Long guildId, Long channelId) {
		ServerConfiguration config = configService.getConfig(guildId);
		DefinedChannelId id = new DefinedChannelId(channelId, config);
		return channelRepo.findById(id).orElse(null);
	}
	
	@Transactional
	public boolean addOrRemove(Long guildId, Long channelId) {
		DefinedChannel channelWatch = get(guildId, channelId);
		if (channelWatch == null) {
			return add(guildId, channelId);
		} else {
			return remove(guildId, channelId);
		}
	}
	
	@Transactional
	public boolean add(Long guildId, Long channelId) {
		DefinedChannel channelWatch = get(guildId, channelId);
		if (channelWatch == null) {
			channelWatch = new DefinedChannel();
			channelWatch.setId(new DefinedChannelId(channelId, configService.getConfig(guildId)));
			channelRepo.save(channelWatch);
			m_logger.debug("Guild {} added channel {}", guildId, channelId);
			return true;
		} else {
			return false;
		}
	}
	
	@Transactional
	public boolean remove(Long guildId, Long channelId) {
		DefinedChannel channelWatch = get(guildId, channelId);
		if (channelWatch == null) {
			return false;
		} else {
			channelRepo.delete(channelWatch);
			m_logger.debug("Guild {} deleted channel {} successfully)", guildId, channelId);
			return true;
		}
	}
	
	@Transactional
	public Long delAll(Long guildId) {
		Long deleted = channelRepo.deleteByIdGuildId(guildId);
		m_logger.debug("Guild {} deleted all channels list (count {})", guildId, deleted);
		return deleted;
	}
	
	@Transactional
	public List<DefinedChannel> getAll(Long guildId) {
		List<DefinedChannel> list = channelRepo.findByIdGuildId(guildId);
		m_logger.trace("Guild {} displayed all channels in list (count {})", guildId, list.size());
		return list;
	}
	
	@Transactional
	public boolean channelEnabled(Long guildId, Long channelId) {
		for (DefinedChannel c : getAll(guildId)) {
			m_logger.trace("{} vs {}", c.getId().getChannelId(), channelId);
			if (c.getId().getChannelId().compareTo(channelId) == 0)
				return true;
		}
		return false;
	}

}
