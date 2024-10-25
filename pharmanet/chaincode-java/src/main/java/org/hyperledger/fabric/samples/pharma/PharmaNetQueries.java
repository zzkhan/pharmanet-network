package org.hyperledger.fabric.samples.pharma;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.samples.pharma.adapter.DrugAdapter;
import org.hyperledger.fabric.samples.pharma.adapter.DrugSaleRecordAdapter;
import org.hyperledger.fabric.samples.pharma.helper.DrugHelper;
import org.hyperledger.fabric.samples.pharma.model.Drug;
import org.hyperledger.fabric.samples.pharma.model.DrugSaleLookupResponse;
import org.hyperledger.fabric.samples.pharma.model.PharmaErrors;
import org.hyperledger.fabric.samples.pharma.model.PharmaNamespaces;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ledger.CompositeKey;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.util.ArrayList;
import java.util.List;


@Contract(
        name = "pharmanet-queries",
        info = @Info(
                title = "Pharmanet Queries",
                description = "Query functions to retrieve world state",
                version = "0.0.1-SNAPSHOT"))
@Slf4j
public class PharmaNetQueries implements ContractInterface {


  /**
   * finds drug sale record based on input key
   *
   * @param ctx
   * @param drugRetailRecordKey
   * @return
   */
  @Transaction(intent = Transaction.TYPE.EVALUATE)
  public DrugSaleLookupResponse verifyDrugSale(final Context ctx, final String drugRetailRecordKey) {

    DrugSaleLookupResponse.DrugSaleLookupResponseBuilder responseBuilder = DrugSaleLookupResponse.builder();
    log.info("looking you drug sale record for key {}", drugRetailRecordKey);
    String drugSaleBytes = ctx.getStub().getStringState(drugRetailRecordKey);
    if (StringUtils.isNotBlank(drugSaleBytes)) {
      log.info("Found drug sale record for key {}", drugRetailRecordKey);
      log.info("drugSaleBytes {}", drugSaleBytes);
      return responseBuilder
              .drugSaleFound(true)
              .drugSaleRecord(DrugSaleRecordAdapter.fromJson(drugSaleBytes))
              .build();
    }
    log.warn("Drug sale record for key {} was not found.", drugRetailRecordKey);
    return responseBuilder.build();
  }

  /**
   * Retrieves an asset with the specified ID from the ledger.
   *
   * @param ctx   the transaction context
   * @param tagId the ID of the asset
   * @return the asset found on the ledger if there was one
   */
  @Transaction(intent = Transaction.TYPE.EVALUATE)
  public Drug ReadDrug(final Context ctx, final String drugName, final String tagId) {
    if(!DrugHelper.drugExists(ctx, drugName, tagId)){
      log.warn(PharmaErrors.ASSET_NOT_FOUND.message("Drug", String.format("%s-%s", drugName, tagId)));
    }
    return DrugHelper.getDrug(ctx, drugName, tagId);
  }

  /**
   * Retrieves an asset with the specified ID from the ledger.
   *
   * @param ctx   the transaction context
   * @return the asset found on the ledger if there was one
   */
  @Transaction(intent = Transaction.TYPE.EVALUATE)
  public Drug[] ReadAllDrugs(final Context ctx) {

    CompositeKey partialDrugsKey = ctx.getStub().createCompositeKey(PharmaNamespaces.DRUG.getPrefix());
    QueryResultsIterator<KeyValue> drugsIterator = ctx.getStub().getStateByPartialCompositeKey(partialDrugsKey);
    List<Drug> allDrugs = new ArrayList<>();
    for (KeyValue result : drugsIterator) {
      allDrugs.add(DrugAdapter.fromJson(result.getStringValue()));
    }

    log.info("returning all drugs {}", allDrugs);
    return allDrugs.toArray(new Drug[0]);
  }
}
