package com.cegeka.ventouris.groovy.coda

import groovy.transform.ToString

@ToString(includeNames = true, includeFields = true)
class MovementRecord {

    String ref = null
    // Continuous sequence number. Starts at 0001 and is increased by 1 for each movement record referring to another movement on the daily statement of account.
    String ref_move = null
    // Detail number for each movement record for the same continuous sequence number.
    String ref_move_detail = null
    String transaction_ref = null
    Double transaction_amount = null
    String transaction_amount_sign = null
    String transaction_type = null
    String transaction_date = null
    String transaction_family = null
    String transaction_code = null
    String transaction_category = null
    Boolean communication_is_structured = null
    String communication_type = null
    String communication = null
    String entry_date = null
    String type = null
    String globalisation_code = null
    String payment_reference = null
    String counterparty_bic = null
    String counterparty_number = null
    String counterparty_name = null
    String counterparty_address = null
    String counterparty_currency = null
}