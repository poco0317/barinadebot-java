package bar.barinade.barry.brain.data;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

import bar.barinade.barry.brain.data.pk.AssociationId;

@Entity
@Table(name = "associations")
public class Association {

	@EmbeddedId
	private AssociationId id;
	
	@Column(name = "count", nullable = false)
	private Long count;

	public AssociationId getId() {
		return id;
	}

	public void setId(AssociationId id) {
		this.id = id;
	}

	public Long getCount() {
		return count;
	}

	public void setCount(Long count) {
		this.count = count;
	}

	@Override
	public int hashCode() {
		return Objects.hash(count, id);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Association other = (Association) obj;
		return Objects.equals(count, other.count) && Objects.equals(id, other.id);
	}
	
}
