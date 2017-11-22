package com.cegeka.ventouris.groovy.coda.parser

import com.cegeka.ventouris.groovy.coda.InformationRecord
import com.cegeka.ventouris.groovy.coda.MovementRecord
import com.cegeka.ventouris.groovy.coda.MovementRecordType
import com.cegeka.ventouris.groovy.coda.Statement
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import static org.assertj.core.api.Assertions.assertThat

class CodaParserTest {

    @Test
    void parse() {
        String coda =
                """|0000017111772505        00038646  XERIUS SOCIAAL VERZEKERINGKREDBEBB   00410682657 00000                                       2
                   |12221BE13410064167139                  EUR0000000021298880161117XERIUS SOCIAAL VERZEKERINGKBC-Bedrijfsrekening               221
                   |2100010000XHIP42440BSCTBBEONTVA00000000000314801711170015000000600970824 / A / HIMALAYA G.C.V.  / XR5413           17111722101 0
                   |2200010000                                                     045000002621 ORBV0006              BBRUBEBB                   1 0
                   |2300010000BE04630433975231                     VAN NOTEN . PARTNERS BV                                                       0 1
                   |3100010001XHIP42440BSCTBBEONTVA001500001001VAN NOTEN . PARTNERS BV                                                           1 0
                   |3200010001AMERIKALEI 207                     2000        ANTWERPEN                                                           0 0
                   |8221BE13410064167139                  EUR0000000044755890171117                                                                0
                   |9               000325000000019303880000000042760890                                                                           2
                   |""".stripMargin()

        def statement = CodaParser.parse(coda).first()

        assertThat(statement.creation_date).isEqualTo('17/11/17')
        assertThat(statement.separate_application).isEqualTo('00000')
        assertThat(statement.version).isEqualTo('2')
        assertThat(statement.acc_number).isEqualTo('BE13410064167139')
        assertThat(statement.currency).isEqualTo('EUR')
        assertThat(statement.description).isEqualTo('KBC-Bedrijfsrekening')
        assertThat(statement.old_balance).isEqualTo(21298.88d)
        assertThat(statement.old_balance_amount_sign).isEqualTo('0')
        assertThat(statement.old_balance_date).isEqualTo('16/11/17')
        assertThat(statement.account_holder_name).isEqualTo('XERIUS SOCIAAL VERZEKERING')
        assertThat(statement.paper_seq_number).isEqualTo('221')
        assertThat(statement.coda_seq_number).isEqualTo('221')
        assertThat(statement.new_balance).isEqualTo(44755.888d)
        assertThat(statement.new_balance_amount_sign).isEqualTo('0')
        assertThat(statement.new_balance_date).isEqualTo('17/11/17')
        assertThat(statement.new_balance_paper_seq_number).isEqualTo('221')
        assertThat(statement.free_comunications).isEmpty()

        def movement = statement.movements.last()

        assertThat(movement.ref).isEqualTo('00010000')
        assertThat(movement.ref_move).isEqualTo('0001')
        assertThat(movement.ref_move_detail).isEqualTo('0000')
        assertThat(movement.transaction_ref).isEqualTo('XHIP42440BSCTBBEONTVA')
        assertThat(movement.transaction_amount).isEqualTo(31.48d)
        assertThat(movement.transaction_amount_sign).isEqualTo('0')
        assertThat(movement.transaction_type).isEqualTo('0')
        assertThat(movement.transaction_date).isEqualTo('17/11/17')
        assertThat(movement.transaction_family).isEqualTo('01')
        assertThat(movement.transaction_code).isEqualTo('50')
        assertThat(movement.transaction_category).isEqualTo('000')
        assertThat(movement.communication_is_structured).isFalse()
        assertThat(movement.communication_type).isNull()
        assertThat(movement.communication).isEqualTo('0600970824 / A / HIMALAYA G.C.V. / XR5413  ')
        assertThat(movement.entry_date).isEqualTo('17/11/17')
        assertThat(movement.type).isEqualTo(MovementRecordType.NORMAL.name())
        assertThat(movement.globalisation_code).isEqualTo('0')
        assertThat(movement.payment_reference).isEqualTo('045000002621 ORBV0006')
        assertThat(movement.counterparty_bic).isEqualTo('BBRUBEBB')
        assertThat(movement.counterparty_number).isEqualTo('BE04630433975231')
        assertThat(movement.counterparty_name).isEqualTo('VAN NOTEN . PARTNERS BV')
        assertThat(movement.counterparty_address).isNull()
        assertThat(movement.counterparty_currency).isEqualTo('')

        InformationRecord information = statement.informations.last()

        assertThat(information.ref).isEqualTo('00010001')
        assertThat(information.transaction_ref).isEqualTo('XHIP42440BSCTBBEONTVA')
        assertThat(information.transaction_type).isEqualTo('0')
        assertThat(information.transaction_family).isEqualTo('01')
        assertThat(information.transaction_code).isEqualTo('50')
        assertThat(information.transaction_category).isEqualTo('000')
        assertThat(information.communication).isEqualTo('001VAN NOTEN . PARTNERS BVAMERIKALEI 207 2000 ANTWERPEN')
    }
}