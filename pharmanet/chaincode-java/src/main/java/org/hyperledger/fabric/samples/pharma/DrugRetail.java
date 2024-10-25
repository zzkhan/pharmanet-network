package org.hyperledger.fabric.samples.pharma;

import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.samples.pharma.adapter.DrugAdapter;
import org.hyperledger.fabric.samples.pharma.adapter.DrugSaleRecordAdapter;
import org.hyperledger.fabric.samples.pharma.helper.DrugHelper;
import org.hyperledger.fabric.samples.pharma.helper.DrugVerificationOutcomeHelper;
import org.hyperledger.fabric.samples.pharma.helper.PharmaAssetKeyHelper;
import org.hyperledger.fabric.samples.pharma.model.Drug;
import org.hyperledger.fabric.samples.pharma.model.DrugSaleRecord;
import org.hyperledger.fabric.samples.pharma.model.PharmaErrors;
import org.hyperledger.fabric.samples.pharma.model.PharmaOrgRoles;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ledger.KeyModification;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.TreeSet;

import static org.hyperledger.fabric.samples.pharma.model.DrugStatus.RETAILED;


@Contract(
        name = "drug-retail",
        info = @Info(
                title = "Drug retail",
                description = "Drug retail",
                version = "0.0.1-SNAPSHOT"))
@Slf4j
public class DrugRetail implements ContractInterface {

  @Transaction(intent = Transaction.TYPE.SUBMIT)
  public String retailDrug(final Context ctx, final String drugName, final String tagId) {

    var requestingOrg = PharmaOrgRepository.getOrg(ctx, ctx.getClientIdentity().getMSPID());
    log.info("Processing retail of drug {}-{} from retailer: {}", tagId, drugName, requestingOrg);

    DrugHelper.verifyRequestorRole(requestingOrg, PharmaOrgRoles.RETAILER, "retailDrug");
    DrugHelper.verifyDrugState(ctx, drugName, tagId, requestingOrg, "retailDrug");

    var drugBeingRetailed = DrugHelper.getDrug(ctx, drugName, tagId);
    if(RETAILED == drugBeingRetailed.getStatus()){
      throw new ChaincodeException(
              String.format("drug %s-%s has already been retailed.", drugName, tagId),
              PharmaErrors.DRUG_ALREADY_RETAILED.message(String.format("%s-%s", drugName, tagId)));
    }

    var maybeVerificationOutcome = DrugVerificationOutcomeHelper.findVerificationOutcomeByVerifier(ctx, tagId, requestingOrg);
    maybeVerificationOutcome.orElseThrow(()
            -> new ChaincodeException("Verification record not found for drug %s-%s and verifier party %s.",
            PharmaErrors.DRUG_VERIFICATION_STATE_INVALID.message(String.format("%s-%s", drugName, tagId), requestingOrg.getName())));

    drugBeingRetailed.setStatus(RETAILED);
    drugBeingRetailed.setUpdatedDate(LocalDateTime.ofInstant(ctx.getStub().getTxTimestamp(), ZoneId.systemDefault()));
    DrugHelper.saveDrug(ctx, drugBeingRetailed);

    var drugKey = PharmaAssetKeyHelper.drugAssetKey(ctx, drugName, tagId);
    var drugHistoryItr = ctx.getStub().getHistoryForKey(drugKey.toString());
    var sortedDrugHistory = new TreeSet<>(Comparator.comparing(Drug::getUpdatedDate));
    for (KeyModification keyModification : drugHistoryItr) {
      sortedDrugHistory.add(DrugAdapter.fromJson(keyModification.getStringValue()));
    }
    var transactionId = ctx.getStub().getTxId();
    var drugSaleRecord = DrugSaleRecord.builder()
            .transactionId(transactionId)
            .retailDateTime(LocalDateTime.ofInstant(ctx.getStub().getTxTimestamp(), ZoneId.systemDefault()))
            .retailer(requestingOrg.getName())
            .drug(drugBeingRetailed)
            .drugHistory(sortedDrugHistory)
            .build();

    var drugSaleKey = PharmaAssetKeyHelper.drugSaleKey(ctx, transactionId);
    log.info("drugSaleRecordKey {}", drugSaleKey.toString());
    ctx.getStub().putStringState(drugSaleKey.toString(), DrugSaleRecordAdapter.serialize(drugSaleRecord));

    return drugSaleKey.toString();
  }
}
