package com.example.myheart.ui.familyalert

import org.junit.Assert.assertEquals
import org.junit.Test

class FamilyAlertActionsTest {

    @Test
    fun `telefone formatado fica so com digitos`() {
        assertEquals("88999998888", sanitizePhoneForSms("(88) 99999-8888"))
    }

    @Test
    fun `mais no inicio e mantido`() {
        assertEquals("+5588999998888", sanitizePhoneForSms("+55 88 99999-8888"))
    }

    @Test
    fun `espacos nas pontas sao ignorados`() {
        assertEquals("8899999888", sanitizePhoneForSms("  88 9999-9888  "))
    }

    @Test
    fun `mais no meio do texto e descartado`() {
        assertEquals("5588999998888", sanitizePhoneForSms("55 88 99999-8888+"))
    }

    @Test
    fun `texto vazio continua vazio`() {
        assertEquals("", sanitizePhoneForSms(""))
    }
}
