package com.supplychain.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;

// ************
// * Contract *
// ************
public class ProductStateContract implements Contract {
	// This is used to identify our contract when building a transaction.
	public static final String ID = "com.supplychain.contracts.ProductStateContract";

	// A transaction is valid if the verify() function of the contract of all the
	// transaction's input and output states
	// does not throw an exception.
	@Override
	public void verify(LedgerTransaction tx) {
		System.out.println("Checking Transaction " + tx.getId() + " by " + Thread.currentThread().getName());
	}

	// Used to indicate the transaction's intent.
	public static class Commands implements CommandData {
		public static class Move extends Commands {
			@Override
			public boolean equals(Object obj) {
				return obj instanceof Move;
			}
		}
	}
}