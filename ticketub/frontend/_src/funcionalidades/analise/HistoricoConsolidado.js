import React, { useState, useEffect, useCallback } from 'react';
import { getMetricasIngestao, getComparacaoPeriodos } from '../../logica_do_sistema/services/analiseService';
import './Analise.css';

function HistoricoConsolidado() {
  const getDefaultDates = () => {
    const fim = new Date();
    const inicio = new Date();
    inicio.setDate(fim.getDate() - 30);
    return {
      inicio: inicio.toISOString().split('T')[0],
      fim: fim.toISOString().split('T')[0]
    };
  };

  const [dates, setDates] = useState(getDefaultDates());
  const [metricas, setMetricas] = useState(null);
  const [comparacao, setComparacao] = useState(null);
  const [loading, setLoading] = useState(true);

  const fetchHistorico = useCallback(async () => {
    setLoading(true);
    try {
      const [metricasData, comparacaoData] = await Promise.all([
        getMetricasIngestao(),
        getComparacaoPeriodos(dates.inicio, dates.fim)
      ]);
      setMetricas(metricasData);
      setComparacao(comparacaoData);
    } catch (err) {
      setMetricas(null);
      setComparacao(null);
    } finally {
      setLoading(false);
    }
  }, [dates]);

  useEffect(() => {
    fetchHistorico();
  }, [fetchHistorico]);

  const handleDateChange = (e) => {
    const { name, value } = e.target;
    setDates(prev => ({ ...prev, [name]: value }));
  };

  const renderDesvio = (valorDesvio) => {
    const num = parseFloat(valorDesvio);
    if (isNaN(num)) return null;

    if (num > 5) {
      return <span className="desvio-positivo">▲ {num}%</span>;
    } else if (num < -5) {
      return <span className="desvio-negativo">▼ {num}%</span>;
    }
    return <span className="desvio-neutro">{num}%</span>;
  };

  return (
    <div className="analise-container">
      <div className="analise-card">
        <div className="analise-card-header">
          <h2>Histórico Consolidado</h2>
        </div>
        
        <div className="analise-card-body">
          <div className="historico-filters">
            <div>
              <label htmlFor="inicio" style={{ marginRight: '0.5rem', fontWeight: 500 }}>Início:</label>
              <input 
                type="date" 
                id="inicio" 
                name="inicio" 
                value={dates.inicio} 
                onChange={handleDateChange} 
              />
            </div>
            <div>
              <label htmlFor="fim" style={{ marginRight: '0.5rem', fontWeight: 500 }}>Fim:</label>
              <input 
                type="date" 
                id="fim" 
                name="fim" 
                value={dates.fim} 
                onChange={handleDateChange} 
              />
            </div>
          </div>

          {loading ? (
            <div className="analise-empty-state">A carregar histórico...</div>
          ) : (!metricas && !comparacao) ? (
            <div className="analise-empty-state">Sem dados disponíveis</div>
          ) : (
            <>
              {metricas && (
                <div style={{ marginBottom: '2rem' }}>
                  <h3 style={{ marginBottom: '1rem', fontSize: '1.1rem' }}>Métricas Globais de Ingestão</h3>
                  <div className="historico-metrics-grid">
                    {Object.entries(metricas).map(([key, value]) => (
                      <div key={key} className="historico-metric-box">
                        <span className="historico-metric-label">{key}</span>
                        <span className="historico-metric-value">{String(value)}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {comparacao && (
                <div>
                  <h3 style={{ marginBottom: '1rem', fontSize: '1.1rem' }}>Comparação de Períodos</h3>
                  <div className="historico-metrics-grid">
                    {Object.entries(comparacao).map(([key, data]) => {
                      // Allow for flat primitives or structured objects { valor, desvio }
                      const valor = data && typeof data === 'object' && 'valor' in data ? data.valor : data;
                      const desvio = data && typeof data === 'object' && 'desvio' in data ? data.desvio : undefined;
                      
                      return (
                        <div key={key} className="historico-metric-box">
                          <span className="historico-metric-label">{key}</span>
                          <span className="historico-metric-value">
                            {String(valor)}
                            {desvio !== undefined && renderDesvio(desvio)}
                          </span>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}

export default HistoricoConsolidado;
