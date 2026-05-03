import React, { useState } from 'react';
import { Popup } from 'react-leaflet';
import './StopLiveDataPopup.css';

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
          {slices.map((s, i) => (
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
          ))}
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

  const fetchData = () => {
    if (loading) return;
    setLoading(true);
    setError(null);

    const cacheBuster = Date.now();
    fetch(`http://localhost:8080/api/mapa/paragens/${stop.stopId}/live-data?t=${cacheBuster}`, { cache: 'no-store' })
      .then(res => {
        if (!res.ok) throw new Error(`Status HTTP: ${res.status}`);
        return res.json();
      })
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

  return (
    <Popup
      eventHandlers={{
        add: fetchData,
        remove: () => {
          setData(null);
          setTicketTypes(null);
          setError(null);
        },
      }}
      autoPan={true}
      autoPanPadding={[10, 10]}
      minWidth={220}
      maxWidth={220}
    >
      <div className="popup-premium">
        <header className="popup-brand-header">
          <h3 className="stop-title">{stop.stopName}</h3>
          <div className="live-tag">LIVE DATA</div>
        </header>

        {loading && <div className="loader-ring"><div></div><div></div><div></div><div></div></div>}
        {error && <div className="popup-error-msg">⚠️ {error}</div>}

        {data && data.totalValidations > 0 ? (
          <main className="popup-main-content">
            <div className="premium-card">
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

            <section className="premium-viz-section">
              <h4 className="viz-title">VALIDAÇÕES POR TIPO</h4>
              <DonutChart data={ticketTypes} />
            </section>
          </main>
        ) : data && (
          <div className="popup-empty-state">
            <div className="empty-icon">📂</div>
            <p>Não foram registadas validações nesta paragem.</p>
          </div>
        )}
      </div>
    </Popup>
  );
}

export default StopLiveDataPopup;
