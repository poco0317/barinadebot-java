package bar.barinade.barry.brain.data.pk;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;

public class AssociationId implements Serializable {
	private static final long serialVersionUID = 1L;
	
	@Column(name = "brain_id", nullable = false)
	private Long brain;
	
	@Column(name = "left_key", nullable = false)
	private String left;
	
	@Column(name = "right_key", nullable = false)
	private String right;
	
	@Column(name = "value", nullable = false)
	private String value;
	
	public AssociationId() {}

	public AssociationId(Long brainId, String left, String right, String value) {
		this.brain = brainId;
		this.left = left;
		this.right = right;
		this.value = value;
	}

	public String getLeft() {
		return left;
	}

	public void setLeft(String left) {
		this.left = left;
	}

	public String getRight() {
		return right;
	}

	public void setRight(String right) {
		this.right = right;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public int hashCode() {
		return Objects.hash(brain, left, right, value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AssociationId other = (AssociationId) obj;
		return Objects.equals(brain, other.brain) && Objects.equals(left, other.left)
				&& Objects.equals(right, other.right) && Objects.equals(value, other.value);
	}

	public Long getBrain() {
		return brain;
	}

	public void setBrain(Long brainId) {
		this.brain = brainId;
	}

}
