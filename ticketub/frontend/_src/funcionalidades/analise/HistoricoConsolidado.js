import React, { useState, useEffect, useCallback, useRef } from 'react';
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

function CustomDatePicker({ value, label, onChange }) {
  const [isOpen, setIsOpen] = useState(false);
  
  const parseDate = (str) => {
    if (!str) return new Date();
    const [y, m, d] = str.split('-').map(Number);
    return new Date(y, m - 1, d);
  };

  const currentDate = parseDate(value);
  const [viewYear, setViewYear] = useState(currentDate.getFullYear());
  const [viewMonth, setViewMonth] = useState(currentDate.getMonth());

  const containerRef = useRef(null);

  useEffect(() => {
    function handleClickOutside(event) {
      if (containerRef.current && !containerRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  useEffect(() => {
    const d = parseDate(value);
    setViewYear(d.getFullYear());
    setViewMonth(d.getMonth());
  }, [value]);

  const monthNames = [
    'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
    'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'
  ];

  const currentYear = new Date().getFullYear();
  const years = [];
  for (let y = currentYear - 15; y <= currentYear + 5; y++) {
    years.push(y);
  }

  const changeMonth = (offset) => {
    let newMonth = viewMonth + offset;
    let newYear = viewYear;
    if (newMonth < 0) {
      newMonth = 11;
      newYear -= 1;
    } else if (newMonth > 11) {
      newMonth = 0;
      newYear += 1;
    }
    setViewMonth(newMonth);
    setViewYear(newYear);
  };

  const selectDay = (day) => {
    const pad = (num) => String(num).padStart(2, '0');
    const dateStr = `${viewYear}-${pad(viewMonth + 1)}-${pad(day)}`;
    onChange(dateStr);
    setIsOpen(false);
  };

  const daysInMonth = new Date(viewYear, viewMonth + 1, 0).getDate();
  const firstDayIndex = new Date(viewYear, viewMonth, 1).getDay();

  const prevMonthDaysToShow = [];
  if (firstDayIndex > 0) {
    const prevMonth = viewMonth === 0 ? 11 : viewMonth - 1;
    const prevYear = viewMonth === 0 ? viewYear - 1 : viewYear;
    const daysInPrevMonth = new Date(prevYear, prevMonth + 1, 0).getDate();
    for (let i = firstDayIndex - 1; i >= 0; i--) {
      prevMonthDaysToShow.push(daysInPrevMonth - i);
    }
  }

  const currentMonthDays = [];
  for (let d = 1; d <= daysInMonth; d++) {
    currentMonthDays.push(d);
  }

  const nextMonthDaysToShow = [];
  const totalSlots = 42;
  const remainingSlots = totalSlots - (prevMonthDaysToShow.length + currentMonthDays.length);
  for (let d = 1; d <= remainingSlots; d++) {
    nextMonthDaysToShow.push(d);
  }

  const formatDateDisplay = (dateStr) => {
    if (!dateStr) return '';
    const [y, m, d] = dateStr.split('-');
    return `${d}/${m}/${y}`;
  };

  return (
    <div className="custom-datepicker-container" ref={containerRef}>
      <label className="custom-datepicker-label">{label}</label>
      <div className="custom-datepicker-input-wrapper" onClick={() => setIsOpen(!isOpen)}>
        <span className="custom-datepicker-value">{formatDateDisplay(value)}</span>
        <svg className="custom-datepicker-icon" viewBox="0 0 24 24" width="16" height="16">
          <rect x="3" y="4" width="18" height="18" rx="2" ry="2" fill="none" stroke="currentColor" strokeWidth="2"/>
          <line x1="16" y1="2" x2="16" y2="6" stroke="currentColor" strokeWidth="2"/>
          <line x1="8" y1="2" x2="8" y2="6" stroke="currentColor" strokeWidth="2"/>
          <line x1="3" y1="10" x2="21" y2="10" stroke="currentColor" strokeWidth="2"/>
        </svg>
      </div>

      {isOpen && (
        <div className="custom-datepicker-popover">
          <div className="custom-datepicker-header">
            <button type="button" className="datepicker-nav-btn" onClick={() => changeMonth(-1)}>
              &larr;
            </button>
            <div className="datepicker-selectors">
              <select
                className="datepicker-select datepicker-select-month"
                value={viewMonth}
                onChange={(e) => setViewMonth(Number(e.target.value))}
              >
                {monthNames.map((name, idx) => (
                  <option key={idx} value={idx}>{name}</option>
                ))}
              </select>
              
              <select
                className="datepicker-select datepicker-select-year"
                value={viewYear}
                onChange={(e) => setViewYear(Number(e.target.value))}
              >
                {years.map((yr) => (
                  <option key={yr} value={yr}>{yr}</option>
                ))}
              </select>
            </div>
            <button type="button" className="datepicker-nav-btn" onClick={() => changeMonth(1)}>
              &rarr;
            </button>
          </div>

          <div className="custom-datepicker-weekdays">
            {['D', 'S', 'T', 'Q', 'Q', 'S', 'S'].map((wd, idx) => (
              <span key={idx} className="datepicker-weekday">{wd}</span>
            ))}
          </div>

          <div className="custom-datepicker-days">
            {prevMonthDaysToShow.map((day, idx) => (
              <span key={`prev-${idx}`} className="datepicker-day datepicker-day-filler">
                {day}
              </span>
            ))}

            {currentMonthDays.map((day) => {
              const isSelected =
                currentDate.getDate() === day &&
                currentDate.getMonth() === viewMonth &&
                currentDate.getFullYear() === viewYear;
              
              const isToday =
                new Date().getDate() === day &&
                new Date().getMonth() === viewMonth &&
                new Date().getFullYear() === viewYear;

              return (
                <span
                  key={`curr-${day}`}
                  className={`datepicker-day ${isSelected ? 'datepicker-day-selected' : ''} ${isToday ? 'datepicker-day-today' : ''}`}
                  onClick={() => selectDay(day)}
                >
                  {day}
                </span>
              );
            })}

            {nextMonthDaysToShow.map((day, idx) => (
              <span key={`next-${idx}`} className="datepicker-day datepicker-day-filler">
                {day}
              </span>
            ))}
          </div>

          <div className="custom-datepicker-footer">
            <button
              type="button"
              className="datepicker-footer-btn"
              onClick={() => {
                const today = new Date();
                const pad = (num) => String(num).padStart(2, '0');
                const todayStr = `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`;
                onChange(todayStr);
                setIsOpen(false);
              }}
            >
              Hoje
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

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