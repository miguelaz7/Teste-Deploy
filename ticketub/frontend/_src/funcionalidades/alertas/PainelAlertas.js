import React, { useState, useEffect, useCallback } from 'react';
import { getAlertasAtivos } from '../../logica_do_sistema/services/alertasService';
import './Alertas.css';

function PainelAlertas() {
  const [resumo, setResumo] = useState(null);
  const [loading, setLoading] = useState(true);

  const fetchAlertas = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getAlertasAtivos();
      setResumo(data && typeof data === 'object' ? data : null);
    } catch {
      setResumo(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchAlertas();
    const id = setInterval(fetchAlertas, 30000);
    return () => clearInterval(id);
  }, [fetchAlertas]);

  const renderSeveridade = (s) => {
    const v = String(s || '').toUpperCase();
    if (v === 'CRITICO') return <span className="badge-severidade badge-alta">Crítico</span>;
    if (v === 'AVISO')   return <span className="badge-severidade badge-media">Aviso</span>;
    return <span className="badge-severidade badge-baixa">Normal</span>;
  };

  return (
    <div className="alertas-card">
      <div className="alertas-card-header">
        <h2>Alertas Ativos</h2>
        <button className="btn-resolver" onClick={fetchAlertas}>Atualizar</button>
      </div>
      <div className="alertas-card-body">
        {loading ? (
          <div className="alertas-empty-state">A carregar alertas...</div>
        ) : !resumo ? (
          <div className="alertas-empty-state">Sem dados disponíveis</div>
        ) : (
          <>
            {/* Resumo geral */}
            <div style={{ display: 'flex', gap: '1rem', marginBottom: '1.5rem', flexWrap: 'wrap' }}>
              <div className="historico-metric-box" style={{ minWidth: 140 }}>
                <span className="historico-metric-label">Total Alertas</span>
                <span className="historico-metric-value">{resumo.totalAlertas ?? 0}</span>
              </div>
              <div className="historico-metric-box" style={{ minWidth: 140 }}>
                <span className="historico-metric-label">Severidade</span>
                <span className="historico-metric-value">{renderSeveridade(resumo.severidade)}</span>
              </div>
              <div className="historico-metric-box" style={{ minWidth: 140 }}>
                <span className="historico-metric-label">Janela</span>
                <span className="historico-metric-value">Últimas {resumo.janelaHoras}h</span>
              </div>
            </div>

            {/* Por motivo */}
            {resumo.porMotivo && Object.keys(resumo.porMotivo).length > 0 ? (
              <div>
                <h3 style={{ marginBottom: '1rem', fontSize: '1rem', fontWeight: 600 }}>Distribuição por Motivo</h3>
                <div className="alertas-list">
                  {Object.entries(resumo.porMotivo).map(([motivo, count]) => (
                    <div key={motivo} className="alerta-item">
                      <div className="alerta-info">
                        <span className="alerta-title">{motivo}</span>
                        <span className="alerta-subtitle">Ocorrências: {count}</span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            ) : (
              <div className="alertas-empty-state">
                {resumo.totalAlertas === 0
                  ? 'Não existem alertas ativos nas últimas 24 horas.'
                  : 'Sem detalhes de motivos disponíveis.'}
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}

export default PainelAlertas;