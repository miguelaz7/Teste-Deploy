import React, { useState, useEffect, useMemo, useRef } from 'react';
import { MapContainer, TileLayer, Marker, Popup, Tooltip, useMapEvents } from 'react-leaflet';
import MarkerClusterGroup from 'react-leaflet-cluster';
import 'leaflet/dist/leaflet.css';
import 'leaflet.markercluster/dist/MarkerCluster.css';
import 'leaflet.markercluster/dist/MarkerCluster.Default.css';
import L from 'leaflet';
import './Mapa.css';
import StopLiveDataPopup from './StopLiveDataPopup';

// Fix for default marker icon in Leaflet + React
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
});

const ParagemUniversidade = [41.557583779603355, -8.397569317003903];



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
    fetch('http://localhost:8080/api/mapa/paragens')
      .then(res => res.json())
      .then(data => {
        const grouped = {};
        data.forEach(s => {
          const key = `${s.lat},${s.lon}`;
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
                lon: s.lon + offset
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

  // Click handler to search nearest stop within 50m of click
  const MapEvents = () => {
    useMapEvents({
      click: async (e) => {
        const { lat, lng } = e.latlng;
        try {
          const res = await fetch(`http://localhost:8080/api/mapa/paragens/procura-por-coordenadas?lat=${lat}&lon=${lng}`);
          if (res.ok) {
            const data = await res.json();
            if (data && data.stopId) {
              const matchedStop = stops.find(s => s.stopId === data.stopId);
              if (matchedStop) {
                if (mapRef.current) {
                  mapRef.current.flyTo([matchedStop.lat, matchedStop.lon], 18, { animate: true });
                  setTimeout(() => {
                    const m = markerRefs.current[matchedStop.stopId];
                    if (m) m.openPopup();
                  }, 400);
                }
              }
            }
          }
        } catch (err) {
          console.error('Erro ao buscar paragem por coordenadas:', err);
        }
      }
    });
    return null;
  };

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
                        mapRef.current.flyTo([stop.lat, stop.lon], 18, { animate: true });
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
          <MapEvents />


          <MarkerClusterGroup 
            disableClusteringAtZoom={17} 
            showCoverageOnHover={false}
            iconCreateFunction={createCustomClusterIcon}
          >
            {filteredStops.map(stop => (
              <Marker 
                key={stop.stopId} 
                position={[stop.lat, stop.lon]}
                ref={(r) => { markerRefs.current[stop.stopId] = r; }}
              >
                <StopLiveDataPopup stop={stop} />
              </Marker>
            ))}
          </MarkerClusterGroup>
        </MapContainer>
      </div>
    </div>
  );
}

export default Mapa;


