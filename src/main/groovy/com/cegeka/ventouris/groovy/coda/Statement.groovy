package com.cegeka.ventouris.groovy.coda

import groovy.transform.ToString

@ToString(includeNames=true, includeFields=true)
//Statement of account
class Statement {
    String creation_date = null
    String separate_application = null
    String version = null
    String acc_number = null
    String currency = null
    String description = null
    Double old_balance = null
    String old_balance_amount_sign = null
    String old_balance_date = null
    String account_holder_name = null
    String paper_seq_number = null
    String coda_seq_number = null
    Double new_balance = null
    String new_balance_amount_sign = null
    String new_balance_date = null
    String new_balance_paper_seq_number = null
    List<MovementRecord> movements = []
    List<InformationRecord> informations = []
    List<FreeCommunication> free_comunications = []
}