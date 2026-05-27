import React, { useState, useEffect, useCallback, useRef } from 'react';
import { getMetricasIngestao, getComparacaoPeriodos } from '../../logica_do_sistema/services/analiseService';
import CustomDatePicker from './CustomDatePicker';
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
  if (num > 5)  return <span className="desvio-positivo">▲ {num}%</span>;
  if (num < -5) return <span className="desvio-negativo">▼ {num}%</span>;
  return <span className="desvio-neutro">{num}%</span>;
};

const renderEstadoBadge = (estado) => {
  if (!estado) return '—';
  const est = String(estado).toUpperCase().replace('_', ' ');
  let className = "historico-status-badge status-good";
  if (est.includes("SEM DADOS") || est.includes("NO DATA")) {
    className = "historico-status-badge status-neutral";
  } else if (est.includes("CRITIC") || est.includes("PERIGO") || est.includes("CRÍTICO")) {
    className = "historico-status-badge status-critical";
  } else if (est.includes("ALERT") || est.includes("ATEN")) {
    className = "historico-status-badge status-attention";
  }
  return <span className={className}>{est}</span>;
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

    const hasCombineKeys = 'validas' in obj && 'taxaValidas' in obj && 'quarentena' in obj && 'taxaQuarentena' in obj;

    if (hasCombineKeys) {
      return (
        <div key={key} className="historico-periodo-section">
          <h4 className="historico-periodo-title">{label(key)}</h4>
          <div className="historico-metrics-grid">
            {/* Volume Total */}
            <div className="historico-metric-box">
              <span className="historico-metric-label">{label('volumeIngestao')}</span>
              <span className="historico-metric-value">{String(obj.volumeIngestao)}</span>
            </div>

            {/* Válidas */}
            <div className="historico-metric-box">
              <span className="historico-metric-label">{label('validas')}</span>
              <span className="historico-metric-value">
                {String(obj.validas)}
                <span className="historico-metric-percentage"> ({obj.taxaValidas}%)</span>
              </span>
            </div>

            {/* Quarentena */}
            <div className="historico-metric-box">
              <span className="historico-metric-label">{label('quarentena')}</span>
              <span className="historico-metric-value">
                {String(obj.quarentena)}
                <span className="historico-metric-percentage"> ({obj.taxaQuarentena}%)</span>
              </span>
            </div>

            {/* Duplicados */}
            <div className="historico-metric-box">
              <span className="historico-metric-label">{label('duplicados')}</span>
              <span className="historico-metric-value">{String(obj.duplicados)}</span>
            </div>

            {/* Estado */}
            <div className="historico-metric-box historico-metric-estado">
              <span className="historico-metric-label">{label('estado')}</span>
              <span className="historico-metric-value">
                {renderEstadoBadge(obj.estado)}
              </span>
            </div>
          </div>
        </div>
      );
    }

    const campos = ['volumeIngestao', 'validas', 'quarentena', 'duplicados', 'taxaValidas', 'taxaQuarentena', 'estado'];
    return (
      <div key={key} className="historico-periodo-section">
        <h4 className="historico-periodo-title">{label(key)}</h4>
        <div className="historico-metrics-grid">
          {campos.filter(c => c in obj).map(c => (
            <div key={c} className="historico-metric-box">
              <span className="historico-metric-label">{label(c)}</span>
              <span className="historico-metric-value">
                {c === 'estado' ? renderEstadoBadge(obj[c]) : String(obj[c])}
              </span>
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
            <CustomDatePicker
              label="Início:"
              value={dates.inicio}
              onChange={(val) => setDates(prev => ({ ...prev, inicio: val }))}
            />
            <CustomDatePicker
              label="Fim:"
              value={dates.fim}
              onChange={(val) => setDates(prev => ({ ...prev, fim: val }))}
            />
          </div>

          {loading ? (
            <div className="analise-empty-state">A carregar histórico...</div>
          ) : (
            <>
              {metricas && (
                <div style={{ marginBottom: '2rem' }}>
                  <h3 className="historico-section-title">Métricas Globais de Ingestão</h3>
                  {['ultimaHora', 'ultimas24Horas', 'ultimos7Dias']
                    .filter(k => k in metricas)
                    .map(k => renderPeriodo(k, metricas[k]))}
                </div>
              )}

              {comparacao && (
                <div>
                  <h3 className="historico-section-title">Comparação de Períodos</h3>
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