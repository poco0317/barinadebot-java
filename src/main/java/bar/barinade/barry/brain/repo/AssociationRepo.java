package bar.barinade.barry.brain.repo;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import bar.barinade.barry.brain.data.Association;
import bar.barinade.barry.brain.data.pk.AssociationId;

@Repository
public interface AssociationRepo extends JpaRepository<Association, AssociationId> {

	List<Association> findByIdBrainAndIdLeftAndIdRight(Long brain, String left, String right);
	List<Association> findByIdBrainAndIdLeft(Long brain, String left);
	List<Association> findByIdBrain(Long brain);
	
	long countByIdBrain(Long brain);
	Page<Association> findByIdBrain(Long brain, Pageable pageable);
	
}
