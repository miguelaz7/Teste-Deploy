import React, { useState, useEffect, useCallback } from 'react';
import { getMetricasIngestao, getComparacaoPeriodos } from '../../logica_do_sistema/services/analiseService';
import './Analise.css';

const LABELS = {
  ultimaHora:      'Última Hora',
  ultimas24Horas:  'Últimas 24 Horas',
  ultimos7Dias:    'Últimos 7 Dias',
  volumeIngestao:  'Volume Total',
  validas:         'Válidas',
  quarentena:      'Quarentena',
  duplicados:      'Duplicados',
  taxaValidas:     'Taxa Válidas (%)',
  taxaQuarentena:  'Taxa Quarentena (%)',
  estado:          'Estado',
  periodo1Dias:    'Período 1 (Dias)',
  periodo1Total:   'Total Período 1',
  periodo2Dias:    'Período 2 (Dias)',
  periodo2Total:   'Total Período 2',
  variacaoPercent: 'Variação (%)',
};

const label = (key) => LABELS[key] || key;

const renderDesvio = (num) => {
  if (isNaN(num)) return null;
  if (num > 5)  return <span className="desvio-positivo"> ▲ {num}%</span>;
  if (num < -5) return <span className="desvio-negativo"> ▼ {num}%</span>;
  return <span className="desvio-neutro"> {num}%</span>;
};

function HistoricoConsolidado() {
  const getDefaultDates = () => {
    const fim = new Date();
    const inicio = new Date();
    inicio.setDate(fim.getDate() - 30);
    return {
      inicio: inicio.toISOString().split('T')[0],
      fim:    fim.toISOString().split('T')[0],
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
        getComparacaoPeriodos(dates.inicio, dates.fim),
      ]);
      setMetricas(metricasData);
      setComparacao(comparacaoData);
    } catch {
      setMetricas(null);
      setComparacao(null);
    } finally {
      setLoading(false);
    }
  }, [dates]);

  useEffect(() => { fetchHistorico(); }, [fetchHistorico]);

  const handleDateChange = (e) => {
    const { name, value } = e.target;
    setDates(prev => ({ ...prev, [name]: value }));
  };

  // Renderiza um período (ultimaHora, ultimas24Horas, ultimos7Dias)
  const renderPeriodo = (key, obj) => {
    if (!obj || typeof obj !== 'object') return null;
    const campos = ['volumeIngestao', 'validas', 'quarentena', 'duplicados', 'taxaValidas', 'taxaQuarentena', 'estado'];
    return (
      <div key={key} style={{ marginBottom: '1.5rem' }}>
        <h4 style={{ marginBottom: '0.75rem', color: '#374151', fontWeight: 600 }}>{label(key)}</h4>
        <div className="historico-metrics-grid">
          {campos.filter(c => c in obj).map(c => (
            <div key={c} className="historico-metric-box">
              <span className="historico-metric-label">{label(c)}</span>
              <span className="historico-metric-value">{String(obj[c])}</span>
            </div>
          ))}
        </div>
      </div>
    );
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
              <input type="date" id="inicio" name="inicio" value={dates.inicio} onChange={handleDateChange} />
            </div>
            <div>
              <label htmlFor="fim" style={{ marginRight: '0.5rem', fontWeight: 500 }}>Fim:</label>
              <input type="date" id="fim" name="fim" value={dates.fim} onChange={handleDateChange} />
            </div>
          </div>

          {loading ? (
            <div className="analise-empty-state">A carregar histórico...</div>
          ) : (
            <>
              {metricas && (
                <div style={{ marginBottom: '2rem' }}>
                  <h3 style={{ marginBottom: '1rem', fontSize: '1.1rem' }}>Métricas Globais de Ingestão</h3>
                  {['ultimaHora', 'ultimas24Horas', 'ultimos7Dias']
                    .filter(k => k in metricas)
                    .map(k => renderPeriodo(k, metricas[k]))}
                </div>
              )}

              {comparacao && (
                <div>
                  <h3 style={{ marginBottom: '1rem', fontSize: '1.1rem' }}>Comparação de Períodos</h3>
                  <div className="historico-metrics-grid">
                    {Object.entries(comparacao).map(([key, data]) => {
                      const valor  = data && typeof data === 'object' && 'valor'  in data ? data.valor  : data;
                      const desvio = data && typeof data === 'object' && 'desvio' in data ? data.desvio : undefined;
                      return (
                        <div key={key} className="historico-metric-box">
                          <span className="historico-metric-label">{label(key)}</span>
                          <span className="historico-metric-value">
                            {String(valor)}
                            {desvio !== undefined && renderDesvio(parseFloat(desvio))}
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