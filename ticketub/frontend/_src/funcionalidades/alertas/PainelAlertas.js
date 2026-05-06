import React, { useState, useEffect, useCallback } from 'react';
import { getAlertasAtivos } from '../../logica_do_sistema/services/alertasService';
import './Alertas.css';

function PainelAlertas() {
  const [alertas, setAlertas] = useState(null);
  const [loading, setLoading] = useState(true);

  const fetchAlertas = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getAlertasAtivos();
      // Se não for um array, garantimos que passa a array vazio
      setAlertas(Array.isArray(data) ? data : []);
    } catch (err) {
      setAlertas(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchAlertas();
  }, [fetchAlertas]);

  const handleResolver = (alertaId) => {
    // Simula a resolução do alerta no frontend removendo-o da lista
    if (alertas) {
      setAlertas(alertas.filter(a => (a.id || a.alertaId) !== alertaId));
    }
    // TODO: Num cenário real, faria um POST/PUT ao backend para resolver o alerta
  };

  const renderSeveridade = (severidade) => {
    const s = String(severidade).toUpperCase();
    if (s.includes('ALTA') || s.includes('CRITICA') || s.includes('CRÍTICA')) {
      return <span className="badge-severidade badge-alta">Alta</span>;
    }
    if (s.includes('BAIXA') || s.includes('INFO')) {
      return <span className="badge-severidade badge-baixa">Baixa</span>;
    }
    return <span className="badge-severidade badge-media">Média</span>;
  };

  return (
    <div className="alertas-card">
      <div className="alertas-card-header">
        <h2>Alertas Ativos</h2>
      </div>
      <div className="alertas-card-body">
        {loading ? (
          <div className="alertas-empty-state">A carregar alertas...</div>
        ) : !alertas ? (
          <div className="alertas-empty-state">Sem dados disponíveis</div>
        ) : alertas.length === 0 ? (
          <div className="alertas-empty-state">Não existem alertas ativos no momento.</div>
        ) : (
          <div className="alertas-list">
            {alertas.map((alerta, idx) => {
              // Tentativa de obter as propriedades, assumindo nomes padrão
              const id = alerta.id || alerta.alertaId || idx;
              const titulo = alerta.titulo || alerta.tipo || alerta.mensagem || `Alerta #${id}`;
              const linha = alerta.linha || alerta.routeId || 'Linha Desconhecida';
              const severidade = alerta.severidade || alerta.nivel || 'Media';
              
              return (
                <div key={id} className="alerta-item">
                  <div className="alerta-info">
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                      <span className="alerta-title">{titulo}</span>
                      {renderSeveridade(severidade)}
                    </div>
                    <span className="alerta-subtitle">Linha: {linha}</span>
                  </div>
                  <div>
                    <button 
                      className="btn-resolver"
                      onClick={() => handleResolver(id)}
                    >
                      Resolver
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}

export default PainelAlertas;
