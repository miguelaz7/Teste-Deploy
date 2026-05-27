import React, { useState } from 'react';
import { Popup } from 'react-leaflet';
import './StopLiveDataPopup.css';
import { apiGet } from '../../logica_do_sistema/services/apiClient';

// ── SVG Icons ───────────────────────────────────────────────────────────────
const IconBus = () => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><rect x="4" y="4" width="16" height="12" rx="2" /><path d="M9 20h6" /><path d="M12 16v4" /><path d="M8 8h.01" /><path d="M16 8h.01" /></svg>
);
const IconChart = () => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="M18 20V10" /><path d="M12 20V4" /><path d="M6 20v-6" /></svg>
);
const IconAlert = () => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" /></svg>
);

// ── SVG Donut Chart ────────────────────────────────────────────────────────
const COLORS = ['#6366f1', '#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899'];

function DonutChart({ data: rawData }) {
  const [hovered, setHovered] = useState(null);
  if (!rawData || rawData.length === 0) return null;

  const data = [...rawData].sort((a, b) => b.count - a.count);
  const total = data.reduce((sum, d) => sum + d.count, 0);
  if (total === 0) return null;

  const formatLabel = (txt) => {
    if (!txt) return '';
    const noUnderscore = txt.replace(/_/g, ' ');
    return noUnderscore.charAt(0).toUpperCase() + noUnderscore.slice(1);
  };

  let cumulative = 0;
  const slices = data.map((d, i) => {
    const fraction = d.count / total;
    const startAngle = cumulative * 2 * Math.PI;
    cumulative += fraction;
    const endAngle = cumulative * 2 * Math.PI;

    const x1 = Math.cos(startAngle - Math.PI / 2);
    const y1 = Math.sin(startAngle - Math.PI / 2);
    const x2 = Math.cos(endAngle - Math.PI / 2);
    const y2 = Math.sin(endAngle - Math.PI / 2);
    const largeArc = fraction > 0.5 ? 1 : 0;

    const path = `M 0 0 L ${x1} ${y1} A 1 1 0 ${largeArc} 1 ${x2} ${y2} Z`;
    return { path, color: COLORS[i % COLORS.length], label: d.type, count: d.count, fraction };
  });

  return (
    <div className="donut-flex-row">
      <div className="donut-wrapper">
        <svg viewBox="-1.2 -1.2 2.4 2.4" className="donut-svg">
          {slices.map((s, i) => {
            if (s.fraction >= 0.999) {
              return (
                <circle
                  key={i}
                  cx="0"
                  cy="0"
                  r="1"
                  fill={s.color}
                  style={{
                    transition: 'all 0.3s ease',
                    opacity: hovered && hovered !== s.label ? 0.3 : 1,
                    cursor: 'pointer'
                  }}
                  onMouseEnter={() => setHovered(s.label)}
                  onMouseLeave={() => setHovered(null)}
                />
              );
            }
            return (
              <path
                key={i}
                d={s.path}
                fill={s.color}
                style={{
                  transition: 'all 0.3s ease',
                  opacity: hovered && hovered !== s.label ? 0.3 : 1,
                  cursor: 'pointer'
                }}
                onMouseEnter={() => setHovered(s.label)}
                onMouseLeave={() => setHovered(null)}
              />
            );
          })}
          <circle cx="0" cy="0" r="0.75" fill="white" />
        </svg>
        <div className="donut-center">
          <span className="donut-total-num">
            {hovered ? slices.find(s => s.label === hovered).count : total}
          </span>
          <span className="donut-total-label">
            {hovered
              ? formatLabel(hovered).toUpperCase().split(' ').map((word, i, arr) => (
                <React.Fragment key={i}>
                  {word}
                  {i < arr.length - 1 && <br />}
                </React.Fragment>
              ))
              : 'TOTAL'
            }
          </span>
        </div>
      </div>
      <div className="donut-legend">
        {slices.map((s, i) => (
          <div
            key={i}
            className="legend-row"
            style={{ opacity: hovered && hovered !== s.label ? 0.4 : 1, transition: 'opacity 0.2s' }}
          >
            <span className="legend-dot" style={{ background: s.color }} />
            <span className="legend-text-compact">
              {formatLabel(s.label)}: <strong>{s.count}</strong>
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}

// ── Main Popup Component ────────────────────────────────────────────────────
function StopLiveDataPopup({ stop }) {
  const [data, setData] = useState(null);
  const [ticketTypes, setTicketTypes] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [showHistory, setShowHistory] = useState(false);

  const fetchData = () => {
    if (loading) return;
    setLoading(true);
    setError(null);

    const cacheBuster = Date.now();
    apiGet(`/api/mapa/paragens/${stop.stopId}/live-data?t=${cacheBuster}`)
      .then(json => {
        setData(json);
        if (json.ticketTypeDistribution) {
          const typesArray = Object.entries(json.ticketTypeDistribution).map(([type, count]) => ({
            type,
            count
          }));
          setTicketTypes(typesArray);
        }
      })
      .catch(err => {
        setError('Dados em tempo real indisponíveis neste momento.');
      })
      .finally(() => {
        setLoading(false);
      });
  };

  const formatPeakInterval = (start, end) => {
    if (!start || !end) return '—';
    const s = start.substring(0, 5);
    const e = end.substring(0, 5);
    return `${s} às ${e}`;
  };

  const renderAfluenciaBadge = () => {
    if (!data || !data.afluenciaStatus) return null;
    switch (data.afluenciaStatus) {
      case 'ACIMA_DA_MEDIA':
        return (
          <div className="afluencia-badge alta" title="Afluência acima da média histórica">
            ⭐ Afluência Elevada!
          </div>
        );
      case 'ABAIXO_DA_MEDIA':
        return (
          <div className="afluencia-badge baixa" title="Afluência abaixo da média (possível anomalia ou avaria)">
            ⚠️ Procura Invulgarmente Baixa
          </div>
        );
      default:
        return (
          <div className="afluencia-badge normal" title="Afluência alinhada com o histórico">
            ✅ Procura Normal
          </div>
        );
    }
  };

  const renderHistoryChart = () => {
    if (!data || !data.historico24h) return null;
    const points = Object.entries(data.historico24h);
    const values = points.map(([_, v]) => v);
    const maxVal = Math.max(...values, 1);

    const width = 200;
    const height = 60;
    const paddingLeft = 5;
    const paddingRight = 5;
    const paddingTop = 5;
    const paddingBottom = 5;

    const chartWidth = width - paddingLeft - paddingRight;
    const chartHeight = height - paddingTop - paddingBottom;
    const barWidth = chartWidth / points.length - 1;

    return (
      <div className="history-chart-container" style={{ marginTop: '12px', borderTop: '1px dashed #e2e8f0', paddingTop: '10px' }}>
        <h4 className="viz-title" style={{ fontSize: '10px', color: '#64748b', marginBottom: '8px', textTransform: 'uppercase' }}>
          Histórico 24h (Validações)
        </h4>
        <svg width={width} height={height} style={{ overflow: 'visible' }}>
          {points.map(([hour, val], i) => {
            const x = paddingLeft + i * (chartWidth / points.length);
            const barHeight = (val / maxVal) * chartHeight;
            const y = height - paddingBottom - barHeight;
            return (
              <rect
                key={i}
                x={x}
                y={y}
                width={barWidth}
                height={barHeight}
                fill="#3b82f6"
                rx="1"
                className="history-bar"
                style={{ transition: 'fill 0.2s', cursor: 'pointer' }}
              >
                <title>{`${hour} - ${val} validações`}</title>
              </rect>
            );
          })}
        </svg>
        <div className="chart-labels" style={{ display: 'flex', justifyContent: 'space-between', fontSize: '9px', color: '#94a3b8', marginTop: '4px' }}>
          <span>{points[0][0]}</span>
          <span>{points[Math.floor(points.length / 2)][0]}</span>
          <span>{points[points.length - 1][0]}</span>
        </div>
      </div>
    );
  };

  return (
    <Popup
      eventHandlers={{
        add: fetchData,
        remove: () => {
          setData(null);
          setTicketTypes(null);
          setError(null);
          setShowHistory(false);
        },
      }}
      autoPan={true}
      autoPanPadding={[10, 10]}
      minWidth={240}
      maxWidth={240}
    >
      <div className="popup-premium">
        <header className="popup-brand-header">
          <h3 className="stop-title">{stop.stopName}</h3>
          <div className="live-tag">LIVE DATA</div>
        </header>

        {loading && <div className="loader-ring"><div></div><div></div><div></div><div></div></div>}
        {error && <div className="popup-error-msg">⚠️ {error}</div>}

        {data && (
          <main className="popup-main-content">
            {renderAfluenciaBadge()}

            {data.totalValidations > 0 ? (
              <>
                <div className="premium-card" style={{ marginTop: '8px' }}>
                  <div className="card-header">
                    <IconBus />
                    <span className="card-tag">MAIOR AFLUÊNCIA</span>
                  </div>
                  <div className="card-body">
                    <div className="peak-time-label">HORÁRIO DE PICO</div>
                    <div className="peak-time-val">{formatPeakInterval(data.peakHourWindowStart, data.peakHourWindowEnd)}</div>
                    <div className="afluencia-inline">
                      <span className="afluencia-inline-label">
                        <IconChart />
                        AFLUÊNCIA
                      </span>
                      <span className="afluencia-inline-value">{data.peakHourValidationsPercentageOfStopTotal}%</span>
                    </div>
                  </div>
                </div>

                <div className="premium-stats-grid">
                  <div className="premium-card mini">
                    <div className="card-header warn">
                      <IconAlert />
                      <span className="card-tag">INVÁLIDAS</span>
                    </div>
                    <div className="card-body">
                      <div className="primary-val">{data.invalidValidationsCount}</div>
                      <div className="secondary-val">{data.invalidValidationsPercentageOfStopTotal}%</div>
                    </div>
                  </div>
                </div>

                {data.qualidadeDegradada && (
                  <div className="qualidade-alerta-banner" style={{
                    backgroundColor: '#fee2e2',
                    border: '1px solid #fca5a5',
                    color: '#b91c1c',
                    padding: '6px 10px',
                    borderRadius: '6px',
                    fontSize: '10px',
                    fontWeight: '600',
                    marginTop: '8px',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px'
                  }}>
                    ⚠️ Qualidade Degradada (&gt;2%)
                  </div>
                )}

                {data.topMotivoRejeicao && data.topMotivoRejeicao.length > 0 && (
                  <div style={{
                    marginTop: '8px',
                    backgroundColor: '#fff5f5',
                    border: '1px solid #ffe3e3',
                    borderRadius: '8px',
                    padding: '6px 8px',
                    fontSize: '9px',
                    color: '#9b2c2c'
                  }}>
                    <div style={{ fontWeight: 'bold', color: '#c53030', marginBottom: '2px', textTransform: 'uppercase', fontSize: '9px' }}>
                      Principais Motivos de Erro:
                    </div>
                    {data.topMotivoRejeicao.map((item, idx) => (
                      <div key={idx} style={{ display: 'flex', justifyContent: 'space-between', padding: '1px 0' }}>
                        <span>• {item.motivo}</span>
                        <strong style={{ marginLeft: '4px' }}>{item.total}</strong>
                      </div>
                    ))}
                  </div>
                )}

                <section className="premium-viz-section">
                  <h4 className="viz-title">VALIDAÇÕES POR TIPO</h4>
                  <DonutChart data={ticketTypes} />
                </section>

                <div style={{ marginTop: '12px', display: 'flex', justifyContent: 'center' }}>
                  <button
                    onClick={() => setShowHistory(!showHistory)}
                    style={{
                      background: 'linear-gradient(135deg, #3b82f6, #2563eb)',
                      color: 'white',
                      border: 'none',
                      padding: '6px 12px',
                      borderRadius: '15px',
                      fontSize: '11px',
                      fontWeight: '600',
                      cursor: 'pointer',
                      boxShadow: '0 2px 8px rgba(37, 99, 235, 0.25)',
                      transition: 'all 0.2s',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '4px'
                    }}
                    onMouseEnter={(e) => e.currentTarget.style.transform = 'scale(1.03)'}
                    onMouseLeave={(e) => e.currentTarget.style.transform = 'none'}
                  >
                    {showHistory ? 'Ocultar Histórico' : 'Ver Histórico 24h'}
                  </button>
                </div>

                {showHistory && renderHistoryChart()}
              </>
            ) : (
              <div className="popup-empty-state">
                <div className="empty-icon">📂</div>
                <p>Não foram registadas validações nesta paragem nos últimos 5 minutos.</p>
              </div>
            )}
          </main>
        )}
      </div>
    </Popup>
  );
}

export default StopLiveDataPopup;
