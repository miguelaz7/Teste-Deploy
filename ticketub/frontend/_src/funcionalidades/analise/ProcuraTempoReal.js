import React, { useState, useEffect, useCallback } from 'react';
import { 
  getTempoReal, 
  getPorHorario, 
  getPorLinha, 
  getPorParagem, 
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
};

const label = (key) => LABELS[key] || key;

const fmt = (key, value) => {
  if (value === null || value === undefined) return '—';
  if (key === 'stopId' && !value) return 'Todas';
  if (typeof value === 'number' && key.toLowerCase().includes('percent')) return `${value}%`;
  if (typeof value === 'string' && value.includes('T') && value.includes(':')) {
    try { return new Date(value).toLocaleString('pt-PT'); } catch { return value; }
  }
  return String(value);
};

function ProcuraTempoReal() {
  const [activeTab, setActiveTab] = useState('horario');
  const [liveData, setLiveData] = useState(null);
  const [tabData, setTabData] = useState(null);
  const [loadingLive, setLoadingLive] = useState(true);
  const [loadingTab, setLoadingTab] = useState(true);

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
    if (!dataArray || !Array.isArray(dataArray) || dataArray.length === 0)
      return <div className="analise-empty-state">Sem dados disponíveis</div>;

    const cols = ['chave', 'totalValidacoes', 'totalInvalidas', 'perfilEstudante', 'perfilSenior', 'perfilNormal'];
    const headers = cols.filter(c => c in dataArray[0]);
    const isLinha = activeTab === 'linha';

    return (
      <div className="analise-table-container">
        <table className="analise-table">
          <thead>
            <tr>
              {headers.map(h => <th key={h}>{label(h)}</th>)}
              {isLinha && <th>Ações</th>}
            </tr>
          </thead>
          <tbody>
            {dataArray.map((row, idx) => (
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
                  return <td key={`${idx}-${h}`}>{fmt(h, value)}</td>;
                })}
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
            ))}
          </tbody>
        </table>
      </div>
    );
  };

  return (
    <div className="analise-container">
      {/* Styles local check to give nice row hover effect */}
      <style>{`
        .clickable-row-hover:hover {
          background-color: rgba(59, 130, 246, 0.08) !important;
        }
      `}</style>

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
                    background: 'linear-gradient(135deg, #1e293b, #0f172a)',
                    border: '1px solid #334155',
                    cursor: 'pointer',
                    transition: 'all 0.2s',
                    position: 'relative',
                    overflow: 'hidden',
                    color: '#f8fafc'
                  }}
                  onMouseEnter={e => {
                    e.currentTarget.style.transform = 'translateY(-2px)';
                    e.currentTarget.style.boxShadow = '0 8px 20px rgba(0, 0, 0, 0.3)';
                    e.currentTarget.style.borderColor = '#38bdf8';
                  }}
                  onMouseLeave={e => {
                    e.currentTarget.style.transform = 'none';
                    e.currentTarget.style.boxShadow = 'none';
                    e.currentTarget.style.borderColor = '#334155';
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                    <span style={{ fontSize: '12px', fontWeight: 'bold', color: '#38bdf8' }}>#{idx + 1}</span>
                    <span style={{ fontSize: '11px', color: '#94a3b8' }}>ID: {route.chave}</span>
                  </div>
                  <div style={{ fontWeight: '600', fontSize: '14px', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', marginBottom: '4px' }}>
                    {route.descricao || `Linha ${route.chave}`}
                  </div>
                  <div style={{ fontSize: '13px', color: '#e2e8f0' }}>
                    <strong>{route.totalValidacoes}</strong> validações
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      <div className="analise-card">
        <div className="analise-tabs">
          {['horario','linha','paragem'].map(t => (
            <button key={t}
              className={`analise-tab ${activeTab === t ? 'active' : ''}`}
              onClick={() => setActiveTab(t)}>
              {t === 'horario' ? 'Por Horário' : t === 'linha' ? 'Por Linha' : 'Por Paragem'}
            </button>
          ))}
        </div>
        <div className="analise-card-body">
          {loadingTab
            ? <div className="analise-empty-state">A carregar...</div>
            : renderTable(tabData)}
        </div>
      </div>

      {/* Drill-down Modal (UC05.2) */}
      {selectedRouteId && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          backgroundColor: 'rgba(0, 0, 0, 0.65)',
          backdropFilter: 'blur(6px)',
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          zIndex: 9999,
          padding: '20px'
        }} onClick={() => setSelectedRouteId(null)}>
          <div style={{
            background: 'linear-gradient(135deg, #1e293b, #0f172a)',
            border: '1px solid #334155',
            borderRadius: '16px',
            width: '100%',
            maxWidth: '650px',
            boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.5)',
            color: '#f8fafc',
            overflow: 'hidden'
          }} onClick={e => e.stopPropagation()}>
            {/* Modal Header */}
            <div style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              padding: '20px 24px',
              borderBottom: '1px solid #334155'
            }}>
              <div>
                <h3 style={{ margin: 0, fontSize: '18px', fontWeight: 'bold', color: '#38bdf8' }}>
                  Detalhe Horário em Tempo Real
                </h3>
                <span style={{ fontSize: '13px', color: '#94a3b8' }}>
                  {selectedRouteName}
                </span>
              </div>
              <button 
                onClick={() => setSelectedRouteId(null)}
                style={{
                  background: 'transparent',
                  border: 'none',
                  color: '#94a3b8',
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
                  <h4 style={{ margin: '0 0 16px', fontSize: '14px', color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                    Distribuição de Procura (Validações por Hora)
                  </h4>
                  
                  {/* Custom Bar Chart */}
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    {Object.entries(routeHourlyDetail).map(([hour, val]) => {
                      const maxVal = Math.max(...Object.values(routeHourlyDetail), 1);
                      const pct = (val / maxVal) * 100;
                      return (
                        <div key={hour} style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                          <span style={{ width: '45px', fontSize: '13px', color: '#94a3b8', textAlign: 'right' }}>
                            {String(hour).padStart(2, '0')}:00
                          </span>
                          <div style={{ flex: 1, height: '14px', backgroundColor: '#334155', borderRadius: '4px', overflow: 'hidden' }}>
                            <div style={{
                              width: `${pct}%`,
                              height: '100%',
                              background: 'linear-gradient(90deg, #38bdf8, #0284c7)',
                              borderRadius: '4px',
                              transition: 'width 0.6s ease'
                            }} />
                          </div>
                          <span style={{ width: '60px', fontSize: '13px', fontWeight: '600', color: '#f8fafc' }}>
                            {val} val
                          </span>
                        </div>
                      );
                    })}
                  </div>
                </div>
              ) : (
                <div style={{ textAlign: 'center', color: '#94a3b8' }}>
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