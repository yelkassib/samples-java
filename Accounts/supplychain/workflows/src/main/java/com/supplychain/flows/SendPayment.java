package com.supplychain.flows;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount;
import com.r3.corda.lib.accounts.workflows.services.AccountService;
import com.r3.corda.lib.accounts.workflows.services.KeyManagementBackedAccountService;
import com.sun.istack.NotNull;
import com.supplychain.accountUtilities.NewKeyForAccount;
import com.supplychain.contracts.PaymentStateContract;
import com.supplychain.states.PaymentState;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.flows.CollectSignatureFlow;
import net.corda.core.flows.FinalityFlow;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.FlowSession;
import net.corda.core.flows.InitiatedBy;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.ReceiveFinalityFlow;
import net.corda.core.flows.SignTransactionFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

// ******************
// * Initiator flow *
// ******************
@InitiatingFlow
@StartableByRPC
public class SendPayment extends FlowLogic<String> {

	// private variables
	private String whoAmI;
	private String whereTo;
	private int amount;

	// public constructor
	public SendPayment(String whoAmI, String whereTo, int amount) {
		this.whoAmI = whoAmI;
		this.whereTo = whereTo;
		this.amount = amount;
	}

	@Suspendable
	@Override
	public String call() throws FlowException {
		// grab account service
		AccountService accountService = getServiceHub().cordaService(KeyManagementBackedAccountService.class);
		// grab the account information
		AccountInfo myAccount = accountService.accountInfo(whoAmI).get(0).getState().getData();
		PublicKey myKey = subFlow(new NewKeyForAccount(myAccount.getIdentifier().getId())).getOwningKey();

		AccountInfo targetAccount = accountService.accountInfo(whereTo).get(0).getState().getData();
		AnonymousParty targetAcctAnonymousParty = subFlow(new RequestKeyForAccount(targetAccount));

		// generating State for transfer
		PaymentState output = new PaymentState(amount, new AnonymousParty(myKey), targetAcctAnonymousParty);

		// Obtain a reference to a notary we wish to use.
		/**
		 * METHOD 1: Take first notary on network, WARNING: use for test, non-prod
		 * environments, and single-notary networks only!* METHOD 2: Explicit selection
		 * of notary by CordaX500Name - argument can by coded in flow or parsed from
		 * config (Preferred)
		 *
		 * * - For production you always want to use Method 2 as it guarantees the
		 * expected notary is returned.
		 */
		final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0); // METHOD 1
		// final Party notary =
		// getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"));
		// // METHOD 2

		TransactionBuilder txbuilder = new TransactionBuilder(notary).addOutputState(output).addCommand(
				new PaymentStateContract.Commands.Create(),
				Arrays.asList(targetAcctAnonymousParty.getOwningKey(), myKey));

		// self sign Transaction
		SignedTransaction locallySignedTx = getServiceHub().signInitialTransaction(txbuilder,
				Arrays.asList(getOurIdentity().getOwningKey(), myKey));

		// Collect sigs
		FlowSession sessionForAccountToSendTo = initiateFlow(targetAccount.getHost());
		List<TransactionSignature> accountToMoveToSignature = (List<TransactionSignature>) subFlow(
				new CollectSignatureFlow(locallySignedTx, sessionForAccountToSendTo,
						targetAcctAnonymousParty.getOwningKey()));
		SignedTransaction signedByCounterParty = locallySignedTx.withAdditionalSignatures(accountToMoveToSignature);

		// Finalize
		subFlow(new FinalityFlow(signedByCounterParty, Arrays.asList(sessionForAccountToSendTo).stream()
				.filter(it -> it.getCounterparty() != getOurIdentity()).collect(Collectors.toList())));
		return "Payment send to " + targetAccount.getHost().getName().getOrganisation() + "'s "
				+ targetAccount.getName() + " team.";
	}
}

@InitiatedBy(SendPayment.class)
class SendPaymentResponder extends FlowLogic<Void> {
	// private variable
	private FlowSession counterpartySession;

	// Constructor
	public SendPaymentResponder(FlowSession counterpartySession) {
		this.counterpartySession = counterpartySession;
	}

	@Override
	@Suspendable
	public Void call() throws FlowException {
		subFlow(new SignTransactionFlow(counterpartySession) {
			@Override
			protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
				// Custom Logic to validate transaction.
			}
		});
		subFlow(new ReceiveFinalityFlow(counterpartySession));
		return null;
	}
}
