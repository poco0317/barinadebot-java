package bar.barinade.barry.brain.service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import bar.barinade.barry.brain.data.Association;
import bar.barinade.barry.brain.data.pk.AssociationId;
import bar.barinade.barry.brain.repo.AssociationRepo;

@Service
public class AssociationService {
	
	@Autowired
	private AssociationRepo repo;
	
	public void flush() {
		repo.flush();
	}
	
	@Transactional
	public Association get(Long brainId, String left, String right, String value) {
		return repo.findById(new AssociationId(brainId, left, right, value)).orElse(null);
	}
	
	@Transactional
	public List<Association> getCandidates(Long brainId, String left, String right) {
		return repo.findByIdBrainAndIdLeftAndIdRight(brainId, left, right);
	}
	
	@Transactional
	public List<Association> getCandidates(Long brainId, String left) {
		return repo.findByIdBrainAndIdLeft(brainId, left);
	}
	
	@Transactional
	public Association random(Long brainId) {
		int count = (int) repo.countByIdBrain(brainId);
		if (count <= 0) {
			return null;
		}
		Page<Association> pg = repo.findByIdBrain(brainId, PageRequest.of(ThreadLocalRandom.current().nextInt(0, count), 1));
		if (pg.hasContent()) {
			return pg.getContent().get(0);
		}
		return null;
	}
	
	@Transactional
	public void save(Long brainId, String left, String right, String value) {
		Association existing = get(brainId, left, right, value);
		if (existing == null) {
			AssociationId id = new AssociationId(brainId, left, right, value);
			existing = new Association();
			existing.setId(id);
			existing.setCount(1L);
		}
		existing.setCount(existing.getCount() + 1L);
		repo.save(existing);
	}

}
