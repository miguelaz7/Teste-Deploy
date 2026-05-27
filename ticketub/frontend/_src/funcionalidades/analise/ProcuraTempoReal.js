import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { MapContainer, TileLayer, Circle, Popup, Tooltip } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import { 
  getTempoReal, 
  getPorHorario, 
  getPorLinha, 
  getPorParagem, 
  getPorZona,
  getTop10Linhas, 
  getDetalheHorarioLinha 
} from '../../logica_do_sistema/services/analiseService';
import './Analise.css';

const LABELS = {
  timeGap:                    'Hora de Pico',
  peakAfluenciaPercentage:    'Afluência no Pico (%)',
  invalidCount:               'Inválidas',
  invalidPercentage:          'Taxa de Inválidas (%)',
  total:                      'Total de Validações',
  stopId:                     'Paragem',
  perspectiva:                'Perspectiva',
  chave:                      'Chave',
  totalValidacoes:            'Total Validações',
  totalInvalidas:             'Inválidas',
  perfilEstudante:            'Estudante',
  perfilSenior:               'Sénior',
  perfilNormal:               'Normal',
  actualizadoEm:              'Actualizado em',
  baselineMedia:              'Baseline Histórico',
  variacaoBaseline:           'Variação (%)',
  velocidadeValidacao:        'Velocidade (val/min)',
  tempoRespostaMedio:         'Tempo Resposta Médio',
  stopLat:                    'Latitude',
  stopLon:                    'Longitude',
  zoneId:                     'Zona ID'
};

const label = (key) => LABELS[key] || key;

const fmt = (key, value) => {
  if (value === null || value === undefined) return '—';
  if (key === 'stopId' && !value) return 'Todas';
  if (typeof value === 'number' && key.toLowerCase().includes('percent')) return `${value.toFixed(2)}%`;
  if (key === 'variacaoBaseline' && typeof value === 'number') {
    return `${value > 0 ? '+' : ''}${value.toFixed(1)}%`;
  }
  if (key === 'velocidadeValidacao' && typeof value === 'number') {
    return `${value.toFixed(2)} val/min`;
  }
  if (key === 'tempoRespostaMedio' && typeof value === 'number') {
    return `${value.toFixed(0)} ms`;
  }
  if (key === 'baselineMedia' && typeof value === 'number') {
    return value.toFixed(1);
  }
  if (typeof value === 'string' && value.includes('T') && value.includes(':')) {
    try { return new Date(value).toLocaleString('pt-PT'); } catch { return value; }
  }
  return String(value);
};

function ProcuraTempoReal() {
  const [activeTab, setActiveTab] = useState('horario'); // horario, linha, paragem, zona
  const [liveData, setLiveData] = useState(null);
  const [tabData, setTabData] = useState(null);
  const [loadingLive, setLoadingLive] = useState(true);
  const [loadingTab, setLoadingTab] = useState(true);

  // Filters
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedProfile, setSelectedProfile] = useState('all'); // all, estudante, senior, normal
  const [selectedPeriod, setSelectedPeriod] = useState('all'); // all, peak_morning, peak_afternoon, offpeak

  // UC05.2 - Top 10 and Drill-down states
  const [top10Data, setTop10Data] = useState([]);
  const [loadingTop10, setLoadingTop10] = useState(false);
  const [selectedRouteId, setSelectedRouteId] = useState(null);
  const [selectedRouteName, setSelectedRouteName] = useState('');
  const [routeHourlyDetail, setRouteHourlyDetail] = useState(null);
  const [loadingDrillDown, setLoadingDrillDown] = useState(false);

  const fetchLiveData = useCallback(async (isSilent = false) => {
    if (!isSilent) setLoadingLive(true);
    try {
      const data = await getTempoReal();
      setLiveData(data);
    } catch { setLiveData(null); }
    finally { if (!isSilent) setLoadingLive(false); }
  }, []);

  const fetchTabData = useCallback(async () => {
    setLoadingTab(true);
    try {
      let data;
      if (activeTab === 'horario') {
        data = await getPorHorario();
      } else if (activeTab === 'linha') {
        data = await getPorLinha();
        // Fetch Top 10
        setLoadingTop10(true);
        try {
          const top10 = await getTop10Linhas();
          setTop10Data(top10);
        } catch (err) {
          console.error("Erro ao carregar top 10:", err);
        } finally {
          setLoadingTop10(false);
        }
      } else if (activeTab === 'paragem') {
        data = await getPorParagem();
      } else if (activeTab === 'zona') {
        data = await getPorZona();
      }
      setTabData(data);
    } catch { 
      setTabData(null); 
    } finally { 
      setLoadingTab(false); 
    }
  }, [activeTab]);

  useEffect(() => {
    fetchLiveData();
    const id = setInterval(() => fetchLiveData(true), 5000);
    return () => clearInterval(id);
  }, [fetchLiveData]);

  useEffect(() => { 
    fetchTabData(); 
  }, [fetchTabData]);

  const handleRouteClick = async (routeId, routeDesc) => {
    setSelectedRouteId(routeId);
    setSelectedRouteName(routeDesc || `Linha ${routeId}`);
    setLoadingDrillDown(true);
    setRouteHourlyDetail(null);
    try {
      const detail = await getDetalheHorarioLinha(routeId);
      setRouteHourlyDetail(detail);
    } catch (err) {
      console.error("Erro no drill-down da rota:", err);
    } finally {
      setLoadingDrillDown(false);
    }
  };

  // Filter logic
  const filteredData = useMemo(() => {
    if (!tabData) return [];
    return tabData.filter(row => {
      // 1. Search Query
      const matchSearch = (row.chave && row.chave.toLowerCase().includes(searchQuery.toLowerCase())) || 
                          (row.descricao && row.descricao.toLowerCase().includes(searchQuery.toLowerCase()));
      
      if (!matchSearch) return false;

      // 2. Profile validation check
      if (selectedProfile !== 'all') {
        if (selectedProfile === 'estudante' && row.perfilEstudante === 0) return false;
        if (selectedProfile === 'senior' && row.perfilSenior === 0) return false;
        if (selectedProfile === 'normal' && row.perfilNormal === 0) return false;
      }

      // 3. Operational Period check (simulated based on Hour keys in 'horario' tab)
      if (activeTab === 'horario' && selectedPeriod !== 'all') {
        const hour = parseInt(row.chave, 10);
        if (selectedPeriod === 'peak_morning' && (hour < 7 || hour > 9)) return false;
        if (selectedPeriod === 'peak_afternoon' && (hour < 17 || hour > 19)) return false;
        if (selectedPeriod === 'offpeak' && ((hour >= 7 && hour <= 9) || (hour >= 17 && hour <= 19))) return false;
      }

      return true;
    });
  }, [tabData, searchQuery, selectedProfile, selectedPeriod, activeTab]);

  // Export to JSON/CSV (respecting privacy rules)
  const handleExport = () => {
    if (!filteredData || filteredData.length === 0) return;
    
    const exported = filteredData.map(row => {
      const cleanRow = { ...row };
      delete cleanRow.id; // remove database keys
      return cleanRow;
    });

    const dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(exported, null, 2));
    const downloadAnchor = document.createElement('a');
    downloadAnchor.setAttribute("href", dataStr);
    downloadAnchor.setAttribute("download", `procura_tempo_real_${activeTab}_export.json`);
    document.body.appendChild(downloadAnchor);
    downloadAnchor.click();
    downloadAnchor.remove();
  };

  // Color logic for Heatmap Intensity (cores quentes para alta procura, cores frias para baixa)
  const getColorForDemand = (total) => {
    if (total > 200) return '#ef4444'; // Red (hot)
    if (total > 100) return '#f97316'; // Orange
    if (total > 50)  return '#eab308'; // Yellow
    if (total > 15)  return '#10b981'; // Green
    return '#3b82f6'; // Blue (cold)
  };

  // Coordinates min/max for scale scaling
  const mapNodes = useMemo(() => {
    if (!tabData) return [];
    return tabData.filter(s => s.stopLat && s.stopLon);
  }, [tabData]);

  // Centroid calculator
  const mapCenter = useMemo(() => {
    if (mapNodes && mapNodes.length > 0) {
      const sumLat = mapNodes.reduce((sum, n) => sum + n.stopLat, 0);
      const sumLon = mapNodes.reduce((sum, n) => sum + n.stopLon, 0);
      return [sumLat / mapNodes.length, sumLon / mapNodes.length];
    }
    return [41.5503, -8.4200]; // Braga default
  }, [mapNodes]);

  const renderLive = () => {
    if (!liveData) return <div className="analise-empty-state">Sem dados disponíveis</div>;
    const campos = ['total', 'timeGap', 'peakAfluenciaPercentage', 'invalidCount', 'invalidPercentage'];
    return (
      <div className="historico-metrics-grid">
        {campos.filter(k => k in liveData).map(k => (
          <div key={k} className="historico-metric-box">
            <span className="historico-metric-label">{label(k)}</span>
            <span className="historico-metric-value">{fmt(k, liveData[k])}</span>
          </div>
        ))}
      </div>
    );
  };

  const renderTable = (dataArray) => {
    if (!dataArray || dataArray.length === 0)
      return <div className="analise-empty-state">Ausência de dados. Tente aplicar outros filtros ou aguarde nova picagem. (FA1)</div>;

    // Build headers based on active tab
    const baseHeaders = ['chave', 'totalValidacoes', 'totalInvalidas', 'perfilEstudante', 'perfilSenior', 'perfilNormal'];
    if (activeTab === 'horario') {
      baseHeaders.push('baselineMedia', 'variacaoBaseline');
    } else if (activeTab === 'linha') {
      baseHeaders.push('velocidadeValidacao', 'tempoRespostaMedio');
    } else if (activeTab === 'paragem' || activeTab === 'zona') {
      baseHeaders.push('stopLat', 'stopLon');
    }

    const headers = baseHeaders.filter(h => h in dataArray[0] || h === 'stopLat' || h === 'stopLon');
    const isLinha = activeTab === 'linha';

    return (
      <div className="analise-table-container">
        <table className="analise-table">
          <thead>
            <tr>
              {headers.map(h => <th key={h}>{label(h)}</th>)}
              {activeTab === 'horario' && <th>Desvio</th>}
              {activeTab === 'paragem' && <th>Georef</th>}
              {isLinha && <th>Ações</th>}
            </tr>
          </thead>
          <tbody>
            {dataArray.map((row, idx) => {
              const hasDesvio = row.desvioDetectado;
              const hasCoords = row.stopLat && row.stopLon;
              
              return (
                <tr 
                  key={idx}
                  onClick={isLinha ? () => handleRouteClick(row.chave, row.descricao || row.chave) : undefined}
                  style={isLinha ? { cursor: 'pointer' } : {}}
                  className={isLinha ? 'clickable-row-hover' : ''}
                >
                  {headers.map(h => {
                    let value = row[h];
                    if (h === 'chave' && isLinha && row.descricao) {
                      value = `${row.chave} - ${row.descricao}`;
                    }
                    
                    // Render styling for variance
                    if (h === 'variacaoBaseline' && typeof value === 'number') {
                      const color = value > 20 ? '#dc2626' : (value < -20 ? '#d97706' : '#16a34a');
                      return (
                        <td key={`${idx}-${h}`} style={{ color, fontWeight: 'bold' }}>
                          {fmt(h, value)}
                        </td>
                      );
                    }

                    // Mask/Warning coordinates
                    if ((h === 'stopLat' || h === 'stopLon') && !value) {
                      return <td key={`${idx}-${h}`} style={{ color: '#94a3b8', fontSize: '11px', fontStyle: 'italic' }}>—</td>;
                    }

                    return <td key={`${idx}-${h}`}>{fmt(h, value)}</td>;
                  })}

                  {/* Hourly Deviation Alarm */}
                  {activeTab === 'horario' && (
                    <td>
                      {hasDesvio ? (
                        <span style={{
                          backgroundColor: '#fee2e2',
                          color: '#dc2626',
                          padding: '2px 8px',
                          borderRadius: '4px',
                          fontSize: '11px',
                          fontWeight: 'bold',
                          border: '1px solid #fca5a5'
                        }}>⚠ DESVIO DETETADO (&gt;20%)</span>
                      ) : (
                        <span style={{ color: '#16a34a', fontSize: '11px', fontWeight: '600' }}>Estável</span>
                      )}
                    </td>
                  )}

                  {/* FA4 - Georeferencing incomplete warning */}
                  {activeTab === 'paragem' && (
                    <td>
                      {!hasCoords ? (
                        <span style={{
                          backgroundColor: '#fffbeb',
                          color: '#d97706',
                          padding: '2px 8px',
                          borderRadius: '4px',
                          fontSize: '11px',
                          fontWeight: '600',
                          border: '1px solid #fef3c7'
                        }}>⚠ Incompleto (FA4)</span>
                      ) : (
                        <span style={{ color: '#16a34a', fontSize: '11px', fontWeight: '600' }}>OK</span>
                      )}
                    </td>
                  )}

                  {isLinha && (
                    <td>
                      <button 
                        style={{
                          padding: '4px 8px',
                          borderRadius: '4px',
                          backgroundColor: '#3b82f6',
                          color: 'white',
                          border: 'none',
                          cursor: 'pointer',
                          fontSize: '11px',
                          fontWeight: 'bold'
                        }}
                        onClick={(e) => {
                          e.stopPropagation();
                          handleRouteClick(row.chave, row.descricao || row.chave);
                        }}
                      >
                        Ver Detalhe
                      </button>
                    </td>
                  )}
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    );
  };

  // Real-Time Leaflet Heatmap container
  const renderHeatmap = () => {
    if (!mapNodes || mapNodes.length === 0) {
      return (
        <div className="analise-empty-state">
          Ausência de coordenadas geográficas válidas para renderizar no mapa.
        </div>
      );
    }

    return (
      <div style={{ position: 'relative', marginTop: '16px', background: '#f8fafc', borderRadius: '12px', border: '1px solid #e2e8f0', padding: '16px' }}>
        <h3 style={{ margin: '0 0 4px', fontSize: '14px', color: '#1e293b', fontWeight: 'bold', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
          🗺 Vista Espacial da Procura em Tempo Real (Mapa de Calor)
        </h3>
        <span style={{ fontSize: '12px', color: '#64748b', display: 'block', marginBottom: '16px' }}>
          * Os círculos abaixo representam as localizações georreferenciadas na rede TUB. Cores quentes (Vermelho/Laranja) representam maior afluência de passageiros.
        </span>

        {/* Legend */}
        <div style={{ display: 'flex', gap: '16px', flexWrap: 'wrap', marginBottom: '16px', fontSize: '12px', color: '#64748b' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span style={{ width: '12px', height: '12px', borderRadius: '50%', backgroundColor: '#ef4444', display: 'inline-block' }} />
            Crítico (&gt;200)
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span style={{ width: '12px', height: '12px', borderRadius: '50%', backgroundColor: '#f97316', display: 'inline-block' }} />
            Alto (&gt;100)
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span style={{ width: '12px', height: '12px', borderRadius: '50%', backgroundColor: '#eab308', display: 'inline-block' }} />
            Médio (&gt;50)
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span style={{ width: '12px', height: '12px', borderRadius: '50%', backgroundColor: '#10b981', display: 'inline-block' }} />
            Baixo (&gt;15)
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span style={{ width: '12px', height: '12px', borderRadius: '50%', backgroundColor: '#3b82f6', display: 'inline-block' }} />
            Mínimo (&le;15)
          </div>
        </div>

        <div style={{ width: '100%', height: '400px', borderRadius: '8px', overflow: 'hidden', border: '1px solid #e2e8f0' }}>
          <MapContainer 
            center={mapCenter} 
            zoom={14} 
            style={{ width: '100%', height: '100%', zIndex: 1 }}
            scrollWheelZoom={true}
          >
            <TileLayer
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />

            {mapNodes.map((node) => {
              const color = getColorForDemand(node.totalValidacoes);
              // Calculate circle size relative to total validacoes (minimum 150m radius, max 600m)
              const radius = Math.min(600, Math.max(150, node.totalValidacoes * 15));
              
              return (
                <Circle
                  key={node.chave}
                  center={[node.stopLat, node.stopLon]}
                  radius={radius}
                  pathOptions={{
                    color: color,
                    fillColor: color,
                    fillOpacity: 0.5,
                    weight: 2
                  }}
                >
                  <Tooltip sticky>
                    <strong>{node.descricao || `Código: ${node.chave}`}</strong><br/>
                    Procura: {node.totalValidacoes} validações
                  </Tooltip>
                  <Popup>
                    <div style={{ fontFamily: 'sans-serif', fontSize: '12px', color: '#1e293b' }}>
                      <strong style={{ display: 'block', fontSize: '13px', borderBottom: '1px solid #e2e8f0', paddingBottom: '4px', marginBottom: '6px' }}>
                        {node.descricao || `Código: ${node.chave}`}
                      </strong>
                      <b>Chave ID:</b> {node.chave}<br/>
                      <b>Validações Totais:</b> {node.totalValidacoes}<br/>
                      <b>Inválidas:</b> {node.totalInvalidas}<br/>
                      <hr style={{ margin: '6px 0', borderColor: '#e2e8f0' }} />
                      <b>Perfil Estudante:</b> {node.perfilEstudante}<br/>
                      <b>Perfil Sénior:</b> {node.perfilSenior}<br/>
                      <b>Perfil Normal:</b> {node.perfilNormal}<br/>
                      {node.zoneId && <><b>Zona ID:</b> {node.zoneId}</>}
                    </div>
                  </Popup>
                </Circle>
              );
            })}
          </MapContainer>
        </div>
      </div>
    );
  };

  return (
    <div className="analise-container">
      <style>{`
        .clickable-row-hover:hover {
          background-color: rgba(59, 130, 246, 0.05) !important;
        }
        .analise-filter-bar {
          display: flex;
          gap: 12px;
          flex-wrap: wrap;
          background: #f8fafc;
          padding: 16px;
          border-radius: 12px;
          border: 1px solid #e2e8f0;
          margin-bottom: 8px;
        }
        .analise-filter-group {
          display: flex;
          flex-direction: column;
          gap: 4px;
        }
        .analise-filter-group label {
          font-size: 11px;
          color: #64748b;
          font-weight: bold;
          text-transform: uppercase;
        }
        .analise-filter-input {
          padding: 6px 12px;
          border-radius: 6px;
          border: 1px solid #cbd5e1;
          background-color: #ffffff;
          color: #0f172a;
          font-size: 13px;
          outline: none;
          min-width: 150px;
        }
        .analise-export-btn {
          margin-left: auto;
          align-self: flex-end;
          padding: 8px 16px;
          border-radius: 6px;
          border: none;
          background-color: #10b981;
          color: white;
          font-weight: bold;
          cursor: pointer;
          font-size: 13px;
          transition: background-color 0.2s;
        }
        .analise-export-btn:hover {
          background-color: #059669;
        }
        @media (max-width: 640px) {
          .analise-export-btn {
            margin-left: 0;
            width: 100%;
          }
        }
      `}</style>

      {/* Realtime widgets */}
      <div className="analise-card">
        <div className="analise-card-header">
          <h2>
            Métricas em Tempo Real
            <span className="analise-live-badge">
              <span className="analise-live-dot"></span>Live
            </span>
          </h2>
        </div>
        <div className="analise-card-body">
          {loadingLive && !liveData
            ? <div className="analise-empty-state">A carregar...</div>
            : renderLive()}
        </div>
      </div>

      {/* Global filtering bar */}
      <div className="analise-filter-bar">
        <div className="analise-filter-group">
          <label>Pesquisar ID / Descrição</label>
          <input 
            type="text" 
            placeholder="Ex: L10, Paragem..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="analise-filter-input"
          />
        </div>

        <div className="analise-filter-group">
          <label>Perfil Tarifário</label>
          <select 
            value={selectedProfile}
            onChange={(e) => setSelectedProfile(e.target.value)}
            className="analise-filter-input"
          >
            <option value="all">Todos os Perfis</option>
            <option value="estudante">Estudante</option>
            <option value="senior">Sénior</option>
            <option value="normal">Normal</option>
          </select>
        </div>

        {activeTab === 'horario' && (
          <div className="analise-filter-group">
            <label>Período Operacional</label>
            <select 
              value={selectedPeriod}
              onChange={(e) => setSelectedPeriod(e.target.value)}
              className="analise-filter-input"
            >
              <option value="all">Todo o Dia</option>
              <option value="peak_morning">Hora Ponta Manhã (7h-9h)</option>
              <option value="peak_afternoon">Hora Ponta Tarde (17h-19h)</option>
              <option value="offpeak">Fora de Pico</option>
            </select>
          </div>
        )}

        <button 
          onClick={handleExport}
          className="analise-export-btn"
          disabled={filteredData.length === 0}
          style={filteredData.length === 0 ? { backgroundColor: '#cbd5e1', color: '#94a3b8', cursor: 'not-allowed', opacity: 0.5 } : {}}
        >
          📥 Exportar Resultados (JSON)
        </button>
      </div>

      {activeTab === 'linha' && top10Data && top10Data.length > 0 && (
        <div className="analise-card" style={{ marginBottom: '20px' }}>
          <div className="analise-card-header">
            <h2 style={{ fontSize: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span>🏆</span> Top 10 Linhas com Maior Procura
            </h2>
          </div>
          <div className="analise-card-body" style={{ paddingTop: '10px' }}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: '12px' }}>
              {top10Data.map((route, idx) => (
                <div 
                  key={route.chave} 
                  className="top-route-card"
                  onClick={() => handleRouteClick(route.chave, route.descricao || route.chave)}
                  style={{
                    padding: '12px 16px',
                    borderRadius: '12px',
                    background: 'linear-gradient(135deg, #ffffff, #f8fafc)',
                    border: '1px solid #e2e8f0',
                    cursor: 'pointer',
                    transition: 'all 0.2s',
                    position: 'relative',
                    overflow: 'hidden'
                  }}
                  onMouseEnter={e => {
                    e.currentTarget.style.transform = 'translateY(-2px)';
                    e.currentTarget.style.boxShadow = '0 8px 20px rgba(0, 0, 0, 0.05)';
                    e.currentTarget.style.borderColor = '#3b82f6';
                  }}
                  onMouseLeave={e => {
                    e.currentTarget.style.transform = 'none';
                    e.currentTarget.style.boxShadow = 'none';
                    e.currentTarget.style.borderColor = '#e2e8f0';
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                    <span style={{ fontSize: '12px', fontWeight: 'bold', color: '#3b82f6' }}>#{idx + 1}</span>
                    <span style={{ fontSize: '11px', color: '#64748b' }}>ID: {route.chave}</span>
                  </div>
                  <div style={{ fontWeight: '600', fontSize: '14px', color: '#1e293b', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', marginBottom: '4px' }}>
                    {route.descricao || `Linha ${route.chave}`}
                  </div>
                  <div style={{ fontSize: '13px', color: '#475569' }}>
                    <strong>{route.totalValidacoes}</strong> validações
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Main Perspectives Tab Box */}
      <div className="analise-card">
        <div className="analise-tabs">
          {['horario','linha','paragem','zona'].map(t => (
            <button key={t}
              className={`analise-tab ${activeTab === t ? 'active' : ''}`}
              onClick={() => {
                setActiveTab(t);
                setSearchQuery('');
              }}>
              {t === 'horario' ? 'Por Horário' : t === 'linha' ? 'Por Linha' : t === 'paragem' ? 'Por Paragem' : 'Por Zona Geográfica'}
            </button>
          ))}
        </div>
        <div className="analise-card-body">
          {loadingTab
            ? <div className="analise-empty-state">A carregar...</div>
            : renderTable(filteredData)}
        </div>
      </div>

      {/* Dynamic real-time map visualizer for paragem and zona perspectives */}
      {(activeTab === 'paragem' || activeTab === 'zona') && !loadingTab && renderHeatmap()}

      {/* Drill-down Modal (UC05.2) */}
      {selectedRouteId && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          backgroundColor: 'rgba(0, 0, 0, 0.4)',
          backdropFilter: 'blur(4px)',
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          zIndex: 9999,
          padding: '20px'
        }} onClick={() => setSelectedRouteId(null)}>
          <div style={{
            background: '#ffffff',
            border: '1px solid #e2e8f0',
            borderRadius: '16px',
            width: '100%',
            maxWidth: '650px',
            boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.1)',
            color: '#1e293b',
            overflow: 'hidden'
          }} onClick={e => e.stopPropagation()}>
            {/* Modal Header */}
            <div style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              padding: '20px 24px',
              borderBottom: '1px solid #e2e8f0'
            }}>
              <div>
                <h3 style={{ margin: 0, fontSize: '18px', fontWeight: 'bold', color: '#3b82f6' }}>
                  Detalhe Horário em Tempo Real
                </h3>
                <span style={{ fontSize: '13px', color: '#64748b' }}>
                  {selectedRouteName}
                </span>
              </div>
              <button 
                onClick={() => setSelectedRouteId(null)}
                style={{
                  background: 'transparent',
                  border: 'none',
                  color: '#64748b',
                  fontSize: '24px',
                  cursor: 'pointer',
                  padding: '4px',
                  lineHeight: '1'
                }}
              >&times;</button>
            </div>
            
            {/* Modal Body */}
            <div style={{ padding: '24px', maxHeight: '450px', overflowY: 'auto' }}>
              {loadingDrillDown ? (
                <div style={{ display: 'grid', placeItems: 'center', minHeight: '120px' }}>
                  A carregar detalhe horário...
                </div>
              ) : routeHourlyDetail ? (
                <div>
                  <h4 style={{ margin: '0 0 16px', fontSize: '14px', color: '#64748b', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                    Distribuição de Procura (Validações por Hora)
                  </h4>
                  
                  {/* Custom Bar Chart */}
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    {Object.entries(routeHourlyDetail).map(([hour, val]) => {
                      const maxVal = Math.max(...Object.values(routeHourlyDetail), 1);
                      const pct = (val / maxVal) * 100;
                      return (
                        <div key={hour} style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                          <span style={{ width: '45px', fontSize: '13px', color: '#64748b', textAlign: 'right' }}>
                            {String(hour).padStart(2, '0')}:00
                          </span>
                          <div style={{ flex: 1, height: '14px', backgroundColor: '#f1f5f9', borderRadius: '4px', overflow: 'hidden' }}>
                            <div style={{
                              width: `${pct}%`,
                              height: '100%',
                              background: 'linear-gradient(90deg, #3b82f6, #2563eb)',
                              borderRadius: '4px',
                              transition: 'width 0.6s ease'
                            }} />
                          </div>
                          <span style={{ width: '60px', fontSize: '13px', fontWeight: '600', color: '#1e293b' }}>
                            {val} val
                          </span>
                        </div>
                      );
                    })}
                  </div>
                </div>
              ) : (
                <div style={{ textAlign: 'center', color: '#64748b' }}>
                  Não foi possível obter os detalhes horários para esta linha.
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default ProcuraTempoReal;