package com.cegeka.ventouris.groovy.coda

import groovy.transform.ToString

@ToString(includeNames=true, includeFields=true)
class InformationRecord {
    String ref = null
    String transaction_ref = null
    String transaction_type = null
    String transaction_family = null
    String transaction_code = null
    String transaction_category = null
    String communication = null
}