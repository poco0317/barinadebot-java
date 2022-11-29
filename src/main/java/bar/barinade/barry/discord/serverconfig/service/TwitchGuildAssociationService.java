package bar.barinade.barry.discord.serverconfig.service;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import bar.barinade.barry.discord.serverconfig.data.TwitchGuildAssociation;
import bar.barinade.barry.discord.serverconfig.repo.TwitchGuildAssociationRepo;

@Service
public class TwitchGuildAssociationService {
	
	@Autowired
	private TwitchGuildAssociationRepo repo;
	
	@Transactional
	public TwitchGuildAssociation getByTwitchId(Long id) {
		return repo.findById(id).orElse(null);
	}
	
	@Transactional
	public List<TwitchGuildAssociation> getByGuildId(Long id) {
		return repo.findByGuildId(id);
	}
	
	@Transactional
	public Long deleteByGuildId(Long id) {
		return repo.deleteByGuildId(id);
	}
	
	@Transactional
	public Long deleteByTwitchId(Long id) {
		return repo.deleteByTwitchId(id);
	}
	
	@Transactional
	public List<TwitchGuildAssociation> getAll() {
		return repo.findAll();
	}
	
	@Transactional
	public boolean setTwitchIdForGuild(Long guildId, Long twitchId) {
		TwitchGuildAssociation existing = getByTwitchId(twitchId);
		if (existing != null) {
			// not allowed to assign a twitch id to more than one server
			// although this could be the same as the current server
			// just return false anyways for trying to add a dupe
			return false;
		}
		existing = new TwitchGuildAssociation();
		existing.setGuildId(guildId);
		existing.setTwitchId(twitchId);
		repo.save(existing);
		return true;
	}

}
