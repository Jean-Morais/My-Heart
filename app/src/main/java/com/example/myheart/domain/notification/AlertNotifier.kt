package com.example.myheart.domain.notification

import com.example.myheart.domain.model.AnalyzedReading

/**
 * Contrato de quem publica o aviso de leitura fora do normal. Vive no domínio para o ViewModel e
 * o caso de uso não conhecerem o Android; a implementação real fica na camada de dados.
 */
interface AlertNotifier {
    /**
     * Publica o aviso da leitura. Devolve true se a notificação foi publicada e false se não foi
     * possível (permissão negada, notificações desligadas no aparelho ou canal bloqueado).
     */
    fun notifyAbnormal(reading: AnalyzedReading): Boolean
}
