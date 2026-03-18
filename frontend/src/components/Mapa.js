import React, { useState } from 'react';
import { MapContainer, TileLayer, Marker, Popup, Tooltip } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';
import './Mapa.css';

// Fix for default marker icon in Leaflet + React
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
});

const ParagemUniversidade = [41.557583779603355, -8.397569317003903];

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

  // Sort data descending by count
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

// ── Popup with fetched data ─────────────────────────────────────────────────
function InsightsPopup() {
  const [insights, setInsights] = useState(null);
  const [byType, setByType] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchData = () => {
    if (insights || loading) return;
    setLoading(true);

    Promise.all([
      fetch('http://localhost:8080/api/validations/insights').then(r => {
        if (!r.ok) throw new Error('Erro ao carregar dados');
        return r.json();
      }),
      fetch('http://localhost:8080/api/validations/count-by-type').then(r => {
        if (!r.ok) throw new Error('Erro ao carregar dados');
        return r.json();
      }),
    ])
      .then(([insightsData, typeData]) => {
        setInsights(insightsData);
        setByType(typeData.map(row => ({ type: row[0], count: Number(row[1]) })));
        setLoading(false);
      })
      .catch(err => {
        setError(err.message);
        setLoading(false);
      });
  };

  const formatTime = (raw) => raw ? raw.replace('T', ' ') : '—';

  const formatPeakInterval = (raw) => {
    if (!raw) return '—';
    // Match the hour part from formats like "2025-03-17T08:30:00" or "08:30:00" or "2025-03-17 08:30:00"
    const match = raw.match(/(?:T|\s|^)(\d{1,2}):\d{2}/);
    if (match) {
      const hour = parseInt(match[1], 10);
      const start = hour.toString().padStart(2, '0');
      const end = (hour + 1).toString().padStart(2, '0');
      return `${start}:00 às ${end}:00`;
    }
    // Fallback if no time pattern is found
    return formatTime(raw);
  };

  return (
    <Popup eventHandlers={{ add: fetchData }} autoPan={true} autoPanPadding={[10, 10]} minWidth={220} maxWidth={220}>
      <div className="popup-premium">
        <header className="popup-brand-header">
          <h3 className="stop-title">Paragem U. Minho</h3>
          <div className="live-tag">LIVE DATA</div>
        </header>

        {loading && <div className="loader-ring"><div></div><div></div><div></div><div></div></div>}
        {error && <div className="popup-error-msg">⚠️ {error}</div>}

        {insights && (
          <main className="popup-main-content">
            {/* Primary Stop Info */}
            <div className="premium-card">
              <div className="card-header">
                <IconBus />
                <span className="card-tag">MAIOR AFLUÊNCIA</span>
              </div>
              <div className="card-body">
                <div className="primary-val">{insights.stopName}</div>
                <div className="secondary-val">{formatPeakInterval(insights.timeGap)}</div>
              </div>
            </div>

            {/* Stats Grid */}
            <div className="premium-stats-grid">
              <div className="premium-card mini">
                <div className="card-header highlight">
                  <IconChart />
                  <span className="card-tag">AFLUÊNCIA</span>
                </div>
                <div className="card-body">
                  <div className="accent-val">{insights.peakAfluenciaPercentage}%</div>
                </div>
              </div>
              <div className="premium-card mini">
                <div className="card-header warn">
                  <IconAlert />
                  <span className="card-tag">INVÁLIDAS</span>
                </div>
                <div className="card-body">
                  <div className="primary-val">{insights.invalidCount}</div>
                  <div className="secondary-val">{insights.invalidPercentage}%</div>
                </div>
              </div>
            </div>

            <section className="premium-viz-section">
              <h4 className="viz-title">Validações por tipo</h4>
              <DonutChart data={byType} />
            </section>
          </main>
        )}
      </div>
    </Popup>
  );
}


// ── Main Map Component ──────────────────────────────────────────────────────
function Mapa() {
  return (
    <div className="map-wrapper">
      <div className="map-header">
        <h2>Mapa de Rede</h2>
        <p>Explore as paragens e rotas dos TUB em Braga.</p>
      </div>
      <div className="map-container-inner">
        <MapContainer
          center={ParagemUniversidade}
          zoom={13}
          scrollWheelZoom={true}
          style={{ height: '100%', width: '100%' }}
        >
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          <Marker position={ParagemUniversidade}>
            <Tooltip direction="top" offset={[-15, -2]} opacity={1}>
              Paragem Universidade do Minho
            </Tooltip>
            <InsightsPopup />
          </Marker>
        </MapContainer>
      </div>
    </div>
  );
}

export default Mapa;
