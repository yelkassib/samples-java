package com.supplychain.states;

import java.util.ArrayList;
import java.util.List;

import com.supplychain.contracts.ProductStateContract;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.CommandAndState;
import net.corda.core.contracts.OwnableState;
import net.corda.core.identity.AbstractParty;

// *********
// * State *
// *********
@BelongsToContract(ProductStateContract.class)
public class ProductState implements OwnableState {

	private AbstractParty holder;
	private AbstractParty previousHolder;
	private String emei;
	private List<AbstractParty> participants;

	public ProductState(AbstractParty holder, AbstractParty previousHolder, String emei) {
		super();
		this.holder = holder;
		this.emei = emei;
		this.participants = new ArrayList<AbstractParty>();
		participants.add(holder);
		participants.add(previousHolder);
	}

	public String getEmei() {
		return emei;
	}

	@Override
	public List<AbstractParty> getParticipants() {
		return participants;
	}

	@Override
	public AbstractParty getOwner() {
		return holder;
	}

	@Override
	public CommandAndState withNewOwner(AbstractParty newOwner) {
		return new CommandAndState(new ProductStateContract.Commands.Move(),
				new ProductState(newOwner, this.previousHolder, this.emei));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		ProductState state = (ProductState) o;

		if (holder != null ? !holder.equals(state.holder) : state.holder != null)
			return false;
		if (previousHolder != null ? !previousHolder.equals(state.previousHolder) : state.previousHolder != null)
			return false;
		return !(emei != null ? !emei.equals(state.emei) : state.emei != null);
	}

	@Override
	public int hashCode() {
		int result = holder != null ? holder.hashCode() : 0;
		result = 31 * result + (previousHolder != null ? previousHolder.hashCode() : 0);
		result = 31 * result + (emei != null ? emei.hashCode() : 0);
		return result;
	}
}