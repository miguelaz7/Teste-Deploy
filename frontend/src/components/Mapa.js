import React, { useState, useEffect, useMemo, useRef } from 'react';
import { MapContainer, TileLayer, Marker, Popup, Tooltip } from 'react-leaflet';
import MarkerClusterGroup from 'react-leaflet-cluster';
import 'leaflet/dist/leaflet.css';
import 'leaflet.markercluster/dist/MarkerCluster.css';
import 'leaflet.markercluster/dist/MarkerCluster.Default.css';
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
    if (loading) return;
    setLoading(true);
    setError(null);

    const parseJsonResponse = async (response, label) => {
      if (!response.ok) {
        throw new Error(`${label} (${response.status})`);
      }
      return response.json();
    };

    const cacheBuster = Date.now();

    Promise.allSettled([
      fetch(`http://localhost:8080/api/validations/insights?t=${cacheBuster}`, { cache: 'no-store' })
        .then(r => parseJsonResponse(r, 'Insights indisponivel')),
      fetch(`http://localhost:8080/api/validations/count-by-type?t=${cacheBuster}`, { cache: 'no-store' })
        .then(r => parseJsonResponse(r, 'Tipos indisponivel')),
    ])
      .then(([insightsResult, typeResult]) => {
        if (insightsResult.status === 'fulfilled') {
          setInsights(insightsResult.value);
        }

        if (typeResult.status === 'fulfilled') {
          setByType(typeResult.value.map(row => ({ type: row[0], count: Number(row[1]) })));
        }

        if (insightsResult.status === 'rejected' && typeResult.status === 'rejected') {
          setError('Backend indisponivel em http://localhost:8080');
        } else if (insightsResult.status === 'rejected') {
          setError('Insights indisponivel no momento.');
        } else if (typeResult.status === 'rejected') {
          setError('Grafico por tipo indisponivel no momento.');
        }
      })
      .finally(() => {
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
    <Popup
      eventHandlers={{
        add: fetchData,
        remove: () => {
          setInsights(null);
          setByType(null);
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
                <div className="peak-time-label">Horário de pico</div>
                <div className="peak-time-val">{formatPeakInterval(insights.timeGap)}</div>
                <div className="afluencia-inline">
                  <span className="afluencia-inline-label">
                    <IconChart />
                    Afluência
                  </span>
                  <span className="afluencia-inline-value">{insights.peakAfluenciaPercentage}%</span>
                </div>
              </div>
            </div>

            {/* Stats Grid */}
            <div className="premium-stats-grid">
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

// ── Custom Premium Cluster Icons ────────────────────────────────────────────
const createCustomClusterIcon = (cluster) => {
  const count = cluster.getChildCount();
  
  let gradient = 'linear-gradient(135deg, #10b981, #059669)'; // Small: Emerald
  let shadowColor = 'rgba(16, 185, 129, 0.4)';
  let dimensions = 30;

  if (count > 20) {
    gradient = 'linear-gradient(135deg, #f59e0b, #ea580c)'; // Medium: Amber/Orange
    shadowColor = 'rgba(245, 158, 11, 0.4)';
    dimensions = 36;
  }
  if (count > 80) {
    gradient = 'linear-gradient(135deg, #ef4444, #b91c1c)'; // Large: Red
    shadowColor = 'rgba(239, 68, 68, 0.4)';
    dimensions = 42;
  }

  return L.divIcon({
    html: `
      <div style="
        width: 100%; 
        height: 100%; 
        border-radius: 50%; 
        background: ${gradient};
        display: flex; 
        align-items: center; 
        justify-content: center; 
        color: white; 
        font-weight: 700; 
        font-size: ${dimensions > 35 ? '13px' : '12px'};
        font-family: 'Inter', system-ui, sans-serif;
        box-shadow: 0 4px 15px ${shadowColor}, inset 0 -2px 5px rgba(0,0,0,0.1);
        border: 3px solid white;
      ">
        ${count}
      </div>
    `,
    className: 'custom-cluster-wrapper',
    iconSize: L.point(dimensions, dimensions, true)
  });
};

// ── Main Map Component ──────────────────────────────────────────────────────
function Mapa() {
  const [stops, setStops] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [showDropdown, setShowDropdown] = useState(false);
  const mapRef = useRef(null);
  const markerRefs = useRef({});

  useEffect(() => {
    fetch('http://localhost:8080/api/stops')
      .then(res => res.json())
      .then(data => {
        const grouped = {};
        data.forEach(s => {
          const key = `${s.stopLat},${s.stopLon}`;
          if (!grouped[key]) grouped[key] = [];
          grouped[key].push(s);
        });
        const finalStops = [];
        Object.values(grouped).forEach(group => {
          if (group.length === 1) {
            finalStops.push(group[0]);
          } else {
            // Separa os marcadores ligeiramente para os lados se as coordenadas forem 100% iguais
            const spacing = 0.00015;
            group.forEach((s, idx) => {
              const offset = (idx - (group.length - 1) / 2) * spacing;
              finalStops.push({
                ...s,
                stopLon: s.stopLon + offset
              });
            });
          }
        });
        setStops(finalStops);
      })
      .catch(err => console.error('Erro ao buscar paragens:', err));
  }, []);

  const filteredStops = useMemo(() => {
    if (!searchTerm) return stops;
    const term = searchTerm.toLowerCase();
    return stops.filter(s => s.stopName && s.stopName.toLowerCase().includes(term));
  }, [stops, searchTerm]);

  return (
    <div className="map-wrapper">
      <style>{`
        .custom-cluster-wrapper {
          background: transparent !important;
          border: none !important;
        }
        .custom-cluster-wrapper > div {
          transition: transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow 0.2s ease;
        }
        .custom-cluster-wrapper:hover > div {
          transform: scale(1.15) translateY(-2px);
        }
      `}</style>
      <div className="map-header">
        <h2>Mapa de Rede</h2>
        <p>Explore as paragens e rotas dos TUB em Braga.</p>
      </div>

      <div className="map-container-inner" style={{ position: 'relative' }}>
        {/* Dynamic Island Search Bar */}
        <div style={{ 
          position: 'absolute', 
          top: '24px', 
          left: '50%', 
          transform: 'translateX(-50%)', 
          zIndex: 1000, 
          width: 'calc(100% - 40px)', 
          maxWidth: '450px',
          display: 'flex',
          justifyContent: 'center'
        }}>
          <div style={{ width: '100%', position: 'relative' }}>
            <input 
              type="text" 
              placeholder="Pesquisar por nome de paragem..."
              value={searchTerm}
              onChange={(e) => {
                setSearchTerm(e.target.value);
                setShowDropdown(true);
              }}
              onFocus={() => setShowDropdown(true)}
              onBlur={() => setTimeout(() => setShowDropdown(false), 200)}
              style={{
                width: '100%',
                padding: '14px 40px 14px 24px',
                borderRadius: '30px',
                border: 'none',
                boxShadow: '0 10px 30px rgba(0,0,0,0.15), 0 4px 10px rgba(0,0,0,0.05)',
                fontSize: '16px',
                outline: 'none',
                transition: 'all 0.3s',
                boxSizing: 'border-box'
              }}
            />
            {searchTerm && (
              <button
                onClick={() => {
                  setSearchTerm('');
                  setShowDropdown(false);
                  if (mapRef.current) {
                    mapRef.current.closePopup();
                    mapRef.current.flyTo(ParagemUniversidade, 13, { animate: true });
                  }
                }}
                style={{
                  position: 'absolute',
                  right: '12px',
                  top: '50%',
                  transform: 'translateY(-50%)',
                  background: 'transparent',
                  border: 'none',
                  cursor: 'pointer',
                  padding: '4px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: '#94a3b8'
                }}
                title="Limpar pesquisa"
              >
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
              </button>
            )}
            {showDropdown && searchTerm && filteredStops.length > 0 && (
              <ul style={{
                position: 'absolute',
                top: '100%',
                left: 0,
                right: 0,
                backgroundColor: '#fff',
                borderRadius: '12px',
                boxShadow: '0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1)',
                margin: '8px 0 0',
                padding: '8px 0',
                listStyle: 'none',
                zIndex: 1000,
                maxHeight: '250px',
                overflowY: 'auto',
                border: '1px solid #e2e8f0'
              }}>
                {filteredStops.slice(0, 8).map(stop => (
                  <li 
                    key={stop.stopId}
                    onClick={() => {
                      setSearchTerm(stop.stopName);
                      setShowDropdown(false);
                      if (mapRef.current) {
                        mapRef.current.flyTo([stop.stopLat, stop.stopLon], 18, { animate: true });
                        mapRef.current.once('moveend', () => {
                          const m = markerRefs.current[stop.stopId];
                          if (m) m.openPopup();
                        });
                      }
                    }}
                    style={{
                      padding: '10px 20px',
                      cursor: 'pointer',
                      borderBottom: '1px solid #f8fafc',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '10px',
                      fontSize: '14px',
                      color: '#334155',
                      boxSizing: 'border-box'
                    }}
                    onMouseEnter={(e) => e.currentTarget.style.backgroundColor = '#f1f5f9'}
                    onMouseLeave={(e) => e.currentTarget.style.backgroundColor = 'transparent'}
                  >
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#64748b" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="10" r="3"/><path d="M12 21.7C17.3 17 20 13 20 10a8 8 0 1 0-16 0c0 3 2.7 7 8 11.7z"/></svg>
                    {stop.stopName}
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
        <MapContainer
          ref={mapRef}
          center={ParagemUniversidade}
          zoom={13}
          scrollWheelZoom={true}
          style={{ height: '100%', width: '100%' }}
        >
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />


          <MarkerClusterGroup 
            disableClusteringAtZoom={17} 
            showCoverageOnHover={false}
            iconCreateFunction={createCustomClusterIcon}
          >
            {filteredStops.map(stop => (
              <Marker 
                key={stop.stopId} 
                position={[stop.stopLat, stop.stopLon]}
                ref={(r) => { markerRefs.current[stop.stopId] = r; }}
              >
                <Popup>{stop.stopName}</Popup>
              </Marker>
            ))}
          </MarkerClusterGroup>
        </MapContainer>
      </div>
    </div>
  );
}

export default Mapa;
