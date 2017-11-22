package com.cegeka.ventouris.groovy.coda.parser

import com.cegeka.ventouris.groovy.coda.parser.CodaParserException
import com.cegeka.ventouris.groovy.coda.FreeCommunication
import com.cegeka.ventouris.groovy.coda.InformationRecord
import com.cegeka.ventouris.groovy.coda.MovementRecord
import com.cegeka.ventouris.groovy.coda.MovementRecordType
import com.cegeka.ventouris.groovy.coda.Statement
import groovy.json.JsonOutput

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CodaParser {

    static void main(String[] args) {
        def codaLines = new File(args[0]).text
        def parse = CodaParser.parse(codaLines)
        println JsonOutput.toJson(parse)
    }



    // Check if the given value is a valid coda content
    // Matches the first 24 characters of a CODA file, as defined by the febelfin specifications
    static def is_valid_coda(String value) {
        (value[0..24] =~ /^0{5}\d{9}05[ D] {7}.*$/).matches()
    }

    static List<Statement> parse(String value) {
        if (!is_valid_coda(value)) {
            throw new IllegalArgumentException('The given value is not a valid coda content')
        }
        List<String> recordlist = value.split('\n')
        def statements = []
        Statement statement = null

        for (String line in recordlist) {
            switch (line[0]) {
                case '0':
                    fixes_globalisation_without_details(statement)
                    statement = new Statement()
                    parseHeader(line, statement)
                    statements.add(statement)
                    break
                case '1':
                    parseHeaderDetails(line, statement)
                    break
                case '2':
                    parseMovementRecord(line, statement)
                    break
                case '3':
                    parseInformationRecord(line, statement)
                    break
                case '4':
                    parseFreeCommunication(line, statement)
                    break
                case '8':
                    parseNewBalanceRecord(line, statement)
                    break
                case '9':
                    break
            }
        }
        fixes_globalisation_without_details(statement)

        return statements
    }

//  Change the movement type from globalisation to normal for the last movements
    static def fixes_globalisation_without_details(Statement statement) {
        if (statement?.movements) {
            def mv = statement.movements.last()
            if (mv.type == MovementRecordType.GLOBALISATION) {
                mv.type = MovementRecordType.NORMAL
            }
        }
    }

    static def parseHeader(String line, Statement statement) {
        def version = line[127]
        statement.version = version
        if (!['1', '2'].contains(version)) {
            throw new CodaParserException(code: 'R001', message: String.format('CODA V%s statements are not supported, please contact your bank', statement.version))
        }

        statement.creation_date = toDate(rmspaces(line[5..10]))
        statement.separate_application = rmspaces(line[83..87])
    }

    static def parseHeaderDetails(String line, Statement statement) {

        if (statement.version == '1') {
            statement.acc_number = rmspaces(line[5..16])
            statement.currency = rmspaces(line[18..20])
        } else if (statement.version == '2') {
            switch (line[1]) {

                case '0':  // Belgian bank account BBAN structure
                    statement.acc_number = rmspaces(line[5..16])
                    statement.currency = rmspaces(line[18..20])
                    break
                case '1':  // foreign bank account BBAN structure
                    throw new CodaParserException(code: 'R1001', message: 'Foreign bank accounts with BBAN structure are not supported')
                case '2':  // Belgian bank account IBAN structure
                    statement.acc_number = rmspaces(line[5..20])
                    statement.currency = rmspaces(line[39..41])
                    break
                case '3':  // foreign bank account IBAN structure
                    throw new CodaParserException(code: 'R1002', message: 'Foreign bank accounts with IBAN structure are not supported')
                default:
                    // Something else, not supported
                    throw new CodaParserException(code: 'R1003', message: 'Unsupported bank account structure')
            }
        }
        statement.description = rmspaces(line[90..124])
        statement.old_balance = toFloat(rmspaces(line[43..57])) / 1000
        statement.old_balance_amount_sign = line[42]
        statement.old_balance_date = toDate(rmspaces(line[58..63]))
        statement.account_holder_name = rmspaces(line[64..89])
        statement.paper_seq_number = rmspaces(line[2..4])
        statement.coda_seq_number = rmspaces(line[125..127])
    }

    static def parseMovementRecord(String line, Statement statement) {
        if (line[1] == '1') {
//            # New statement line
            def record = new MovementRecord()
            record.ref = rmspaces(line[2..9])
            record.ref_move = rmspaces(line[2..5])
            record.ref_move_detail = rmspaces(line[6..9])
            record.transaction_ref = rmspaces(line[10..30])
            record.transaction_amount_sign = line[31]  // 0 = Credit, 1 = Debit
            record.transaction_amount = toFloat(rmspaces(line[32..46])) / 1000
            record.transaction_type = Integer.valueOf(line[53])
            record.transaction_date = toDate(rmspaces(line[47..52]))
            record.transaction_family = rmspaces(line[54..55])
            record.transaction_code = rmspaces(line[56..57])
            record.transaction_category = rmspaces(line[58..60])
            record.communication_is_structured = line[61] == '1'
            if (record.communication_is_structured) {
                // Structured communication
                record.communication_type = line[62..64]
                record.communication = '+++' + line[65..67] + '/' + line[68..71] + '/' + line[72..76] + '+++'
            } else {
                // Non-structured communication
                record.communication = rmspaces(line[62..114])
            }
            record.entry_date = toDate(rmspaces(line[115..120]))
            record.type = MovementRecordType.NORMAL

            if ([1, 2, 3].contains(record.transaction_type)) {
                // here the transaction type is a globalisation
                // - 1 is for globalisation from the customer
                // - 2 is for globalisation from the bank
                record.type = MovementRecordType.GLOBALISATION
            }
            // a globalisation record can be followed by details lines
            // if it's not the case, the globalisation record is considered as normal.
            // To determine if a globalisation record is followed by details,
            // on a new line starting with 21, we check if the previous line is a globalisation record.
            // If the current line is not a details line (transaction_type > 3) we reset the record.type to Normal
            MovementRecord prev_mvmt = statement.movements ? statement.movements.last() : null
            if (prev_mvmt != null && record.transaction_type < 4 && prev_mvmt.type == MovementRecordType.GLOBALISATION) {
                prev_mvmt.type = MovementRecordType.NORMAL
            }
            record.globalisation_code = Integer.valueOf(line[124])
            statement.movements.add(record)
        } else if (line[1] == '2') {
            def record = statement.movements[-1]
            if (record.ref[0..3] != line[2..5]) {
                throw new CodaParserException(code: 'R2004', message: String.format('CODA parsing error on movement data record 2.2, seq nr %s!', line[2..9]))
            }
            record.communication = join_communications(record.communication, rmspaces(line[10..62]))
            record.payment_reference = rmspaces(line[63..97])
            record.counterparty_bic = rmspaces(line[98..108])

        } else if (line[1] == '3') {
            def record = statement.movements[-1]
            if (record.ref[0..3] != line[2..5]) {
                throw new CodaParserException(code: 'R2005', message: String.format('CODA parsing error on movement data record 2.3, eq nr %s!', line[2..9]))
            }
            if (statement.version == '1') {
                record.counterparty_number = rmspaces(line[10..21])
                record.counterparty_name = rmspaces(line[47..72])
                record.counterparty_address = rmspaces(line[73..124])
                record.counterparty_currency = ''
            } else {
                if (line[22] == ' ') {
                    record.counterparty_number = rmspaces(line[10..21])
                    record.counterparty_currency = rmspaces(line[23..25])
                } else {

                    record.counterparty_number = rmspaces(line[10..43])
                    record.counterparty_currency = rmspaces(line[44..46])
                }
                record.counterparty_name = rmspaces(line[47..81])
                record.communication = join_communications(record.communication, rmspaces(line[82..124]))
            }
        } else {
//        else:
//            # movement data record 2.x (x != 1,2,3)
            throw new CodaParserException(code: 'R2006', message: String.format('Movement data records of type 2.%s are not supported ', line[1]))
        }
    }

    static def parseInformationRecord(String line, Statement statement) {
        switch (line[1]) {
            case '1':
                def infoLine = new InformationRecord()
                infoLine.ref = rmspaces(line[2..9])
                infoLine.transaction_ref = rmspaces(line[10..30])
                infoLine.transaction_type = line[31]
                infoLine.transaction_family = rmspaces(line[32..33])
                infoLine.transaction_code = rmspaces(line[34..35])
                infoLine.transaction_category = rmspaces(line[36..38])
                infoLine.communication = rmspaces(line[40..112])
                statement.informations.add(infoLine)
                break
            case '2':
                InformationRecord infoLine = statement.informations.last()
                if (infoLine.ref != rmspaces(line[2..9])) {
                    throw new CodaParserException(code: 'R3004', message: String.format('CODA parsing error on information data record 3.2, seq nr %s!', line[2..10]))
                }
                infoLine.communication += rmspaces(line[10..99])
                break
            case '3':
                def infoLine = statement.informations.last()
                if (infoLine.ref != rmspaces(line[2..9])) {
                    throw new CodaParserException(code: 'R3005', message: String.format('CODA parsing error on information data record 3.3, seq nr %s!', line[2..10]))
                }
                infoLine.communication += rmspaces(line[10..99])
                break
        }
    }

    static def parseNewBalanceRecord(String line, Statement statement) {
        statement.new_balance_amount_sign = line[41]
        statement.new_balance_paper_seq_number = rmspaces(line[1..3])
        statement.new_balance = toFloat(rmspaces(line[42..56])) / 1000
        statement.new_balance_date = toDate(rmspaces(line[57..62]))
    }

    static def parseFreeCommunication(String line, Statement statement) {
        def comm_line = new FreeCommunication()
        comm_line.ref = rmspaces(line[2..9])
        comm_line.communication = rmspaces(line[32..111])
        statement.free_comunications.add(comm_line)
    }


    static def join_communications(c1, c2) {
        if (c1 == null) {
            return c2
        }
        if (c2 == null) {
            return c1
        }
        if (!c2.startsWith(" ")) {
            return c1 + " " + c2
        }
        return c1 + c2
    }


    static def rmspaces(String s) {
        s.split().join(" ")
    }

    static def toFloat(String str) {
        str.isAllWhitespace() ? 0 : Float.valueOf(str)
    }

    static def toDate(String string) {
        def date = LocalDate.parse(string, DATE_FORMAT)
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yy"))
    }

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("ddMMyy")
}



