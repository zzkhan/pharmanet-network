function

getResult(txStatus) {
    let json = ''
    let jsonResultString = JSON.stringify(txStatus)
    // console.log(`GETRESULT 1 - ${jsonResultString}`)
    let jsonParsed = JSON.parse(jsonResultString);
    // console.log(`GETRESULT 2 - ${JSON.stringify(jsonParsed)}`)
    // console.log(`GETRESULT 3 - ${jsonParsed.status.status}`)

    if (jsonParsed.status.status === "success") {
        console.log(`GETRESULT - transaction was successful ${txStatus.status}.`);
        if (txStatus.GetResult() !== undefined) {
            const resultBuffer = Buffer.from(txStatus.GetResult());
            if (resultBuffer !== undefined && resultBuffer.length > 0) {
                // Convert the Buffer to a string
                const resultString = resultBuffer.toString('utf8');
                // console.log(`GETRESULT - Raw result: ${resultString}`)
                try {
                    // console.log(`GETRESULT - JSON.stringify: ${JSON.stringify(resultString)}`);
                    json = JSON.parse(resultString)
                    // console.log(`GETRESULT - JSON data: ${json}`);
                    json = JSON.stringify(json)
                } catch (e) {
                    console.warn("GETRESULT - error parsing result as JSON", e.message)
                    json = resultString
                }
            } else {
                console.log('GETRESULT - No result data returned from the transaction.');
            }
        }
    } else {
        console.warn(`GETRESULT - transaction was not successful ${txStatus.status.status}.`);
    }
    return json;
}

function createDrugs(drugNameIndex, drugNameEndIndex, totalDrugInstances) {
    let drugs = []
    let i = 0
    for (i = drugNameIndex; i <= drugNameEndIndex; i++) {
        let j = 0
        for (j = 0; j <= totalDrugInstances - 1; j++) {
            let drugDetails = {
                name: `drug_1_${i}`,
                tagId: `TAG_ID_1_${i}_${j}`
            }
            drugs.push(drugDetails)
        }
    }
    console.log(`Utils - returning drugs list ${JSON.stringify(drugs)} with size ${drugs.length}`)
    return drugs;
}

function

findByTagId(jsonArray, tagId) {
    console.log(`finding tagId ${tagId}`)
    let drugFound = jsonArray.find(item => {
        return item.tagId === tagId
    });
    if (drugFound) {
        console.log(`drugFound ${drugFound}`)
    } else {
        console.log(`drug with tagId ${tagId} NOT FOUND...`)
    }
    return drugFound;
}

module.exports = {getResult, createDrugs, findByTagId};
