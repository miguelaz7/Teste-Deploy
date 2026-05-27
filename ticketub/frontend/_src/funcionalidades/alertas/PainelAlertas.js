import React, { useState, useEffect, useCallback } from 'react';
import { MapContainer, TileLayer, Circle, Popup } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { 
  getAlertasAtivos, 
  getAnomalias, 
  getSumarioAnomalias, 
  getMapaCalor, 
  assignAlerta, 
  resolveAlerta, 
  markFalsePositive, 
  confirmarEscalacao 
} from '../../logica_do_sistema/services/alertasService';
import './Alertas.css';

// Fix Leaflet default icon issues
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
});

function PainelAlertas() {
  const [tab, setTab] = useState('dashboard'); // 'dashboard', 'anomalias', 'heatmap'
  const [resumo, setResumo] = useState(null);
  const [sumario, setSumario] = useState(null);
  const [anomalias, setAnomalias] = useState([]);
  const [hotspots, setHotspots] = useState([]);
  const [loading, setLoading] = useState(true);

  // Form states for resolutions
  const [resolucaoTexto, setResolucaoTexto] = useState({});
  const [avisoRegra, setAvisoRegra] = useState('');

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const dataAtivos = await getAlertasAtivos();
      setResumo(dataAtivos && typeof dataAtivos === 'object' ? dataAtivos : null);

      const dataAnomalias = await getAnomalias();
      setAnomalias(Array.isArray(dataAnomalias) ? dataAnomalias : []);

      const dataSumario = await getSumarioAnomalias();
      setSumario(dataSumario && typeof dataSumario === 'object' ? dataSumario : null);

      const dataCalor = await getMapaCalor();
      setHotspots(Array.isArray(dataCalor) ? dataCalor : []);
    } catch (err) {
      console.error("Erro ao carregar dados de monitorização:", err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
    const interval = setInterval(fetchData, 45000);
    return () => clearInterval(interval);
  }, [fetchData]);

  const handleAtribuir = async (id) => {
    try {
      await assignAlerta(id, 'Fiscal Operacional');
      await fetchData();
    } catch (err) {
      alert('Erro ao atribuir alerta.');
    }
  };

  const handleResolver = async (id) => {
    const texto = resolucaoTexto[id] || 'Verificação concluída';
    try {
      await resolveAlerta(id, texto);
      setResolucaoTexto(prev => ({ ...prev, [id]: '' }));
      await fetchData();
    } catch (err) {
      alert('Erro ao resolver alerta.');
    }
  };

  const handleFalsoPositivo = async (id) => {
    const texto = resolucaoTexto[id] || 'Falso positivo identificado em terreno';
    try {
      const res = await markFalsePositive(id, texto);
      setResolucaoTexto(prev => ({ ...prev, [id]: '' }));
      if (res.revisaoRegrasNecessaria) {
        setAvisoRegra(res.mensagem || 'Revisão recomendada devido a falsos positivos recorrentes.');
      } else {
        setAvisoRegra('');
      }
      await fetchData();
    } catch (err) {
      alert('Erro ao registar falso positivo.');
    }
  };

  const handleConfirmarEscalacao = async (id) => {
    try {
      await confirmarEscalacao(id);
      await fetchData();
    } catch (err) {
      alert('Erro ao confirmar escalação.');
    }
  };

  const renderSeveridade = (s) => {
    const v = String(s || '').toUpperCase();
    if (v === 'CRITICO' || v === 'ALTA' || v === 'ALTO') 
      return <span className="badge-severidade badge-alta">Crítico</span>;
    if (v === 'AVISO' || v === 'MEDIA' || v === 'MEDIO')   
      return <span className="badge-severidade badge-media">Aviso</span>;
    return <span className="badge-severidade badge-baixa">Normal</span>;
  };

  const renderEstado = (status) => {
    const v = String(status || '').toUpperCase();
    if (v === 'RESOLVIDO') return <span style={{ color: '#166534', background: '#dcfce7', padding: '2px 8px', borderRadius: '4px', fontSize: '0.8rem', fontWeight: 600 }}>RESOLVIDO</span>;
    if (v === 'FALSO_POSITIVO') return <span style={{ color: '#374151', background: '#e5e7eb', padding: '2px 8px', borderRadius: '4px', fontSize: '0.8rem', fontWeight: 600 }}>FALSO POSITIVO</span>;
    if (v === 'EM_ANALISE') return <span style={{ color: '#1d4ed8', background: '#dbeafe', padding: '2px 8px', borderRadius: '4px', fontSize: '0.8rem', fontWeight: 600 }}>EM ANÁLISE</span>;
    if (v === 'EM_ESCALACAO') return <span style={{ color: '#991b1b', background: '#fee2e2', padding: '2px 8px', borderRadius: '4px', fontSize: '0.8rem', fontWeight: 600 }}>EM ESCALAÇÃO</span>;
    return <span style={{ color: '#854d0e', background: '#fef9c3', padding: '2px 8px', borderRadius: '4px', fontSize: '0.8rem', fontWeight: 600 }}>PENDENTE</span>;
  };

  return (
    <div className="alertas-card">
      <div className="alertas-card-header">
        <h2>Detecção de Anomalias & Evasão</h2>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <button 
            className={`btn-exportar ${tab === 'dashboard' ? 'active' : ''}`}
            onClick={() => setTab('dashboard')}
            style={{ backgroundColor: tab === 'dashboard' ? '#1e293b' : '#3b82f6' }}
          >
            Painel de Controlo
          </button>
          <button 
            className={`btn-exportar ${tab === 'anomalias' ? 'active' : ''}`}
            onClick={() => setTab('anomalias')}
            style={{ backgroundColor: tab === 'anomalias' ? '#1e293b' : '#3b82f6' }}
          >
            Registo de Anomalias ({anomalias.filter(a => a.status !== 'RESOLVIDO' && a.status !== 'FALSO_POSITIVO').length})
          </button>
          <button 
            className={`btn-exportar ${tab === 'heatmap' ? 'active' : ''}`}
            onClick={() => setTab('heatmap')}
            style={{ backgroundColor: tab === 'heatmap' ? '#1e293b' : '#3b82f6' }}
          >
            Mapa de Calor
          </button>
          <button className="btn-resolver" onClick={fetchData} disabled={loading}>
            <svg style={{ marginRight: '6px' }} className={loading ? "spinning-icon" : ""} width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <path d="M23 4v6h-6"></path>
              <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path>
            </svg>
            Atualizar
          </button>
        </div>
      </div>

      <div className="alertas-card-body">
        {avisoRegra && (
          <div style={{ marginBottom: '1.25rem', padding: '1rem', background: '#fffbeb', borderRadius: 8, color: '#b45309', border: '1px solid #fef3c7', display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
            <span style={{ fontWeight: 700 }}>⚠️ Alerta de Ajuste de Regras (DPO & Admins)</span>
            <span style={{ fontSize: '0.9rem' }}>{avisoRegra}</span>
            <button onClick={() => setAvisoRegra('')} style={{ alignSelf: 'flex-start', background: 'none', border: 'none', color: '#b45309', textDecoration: 'underline', padding: 0, marginTop: '0.25rem', cursor: 'pointer', fontSize: '0.8rem' }}>Dispensar</button>
          </div>
        )}

        {loading && !resumo ? (
          <div className="alertas-empty-state-container">
            <div className="empty-state-icon-wrapper loading">
              <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#3b82f6" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" className="spinning-icon">
                <line x1="12" y1="2" x2="12" y2="6"></line>
                <line x1="12" y1="18" x2="12" y2="22"></line>
                <line x1="4.93" y1="4.93" x2="7.76" y2="7.76"></line>
                <line x1="16.24" y1="16.24" x2="19.07" y2="19.07"></line>
                <line x1="2" y1="12" x2="6" y2="12"></line>
                <line x1="18" y1="12" x2="22" y2="12"></line>
                <line x1="4.93" y1="19.07" x2="7.76" y2="16.24"></line>
                <line x1="16.24" y1="7.76" x2="19.07" y2="4.93"></line>
              </svg>
            </div>
            <h4 className="empty-state-title">A sincronizar dados...</h4>
            <p className="empty-state-description">A ligar ao módulo de monitorização e fiscalização operacional.</p>
          </div>
        ) : tab === 'dashboard' ? (
          <>
            {/* Dashboard metrics */}
            <div className="alertas-metrics-grid">
              <div className="alertas-metric-box">
                <span className="alertas-metric-label">Alertas de Validação (24h)</span>
                <span className="alertas-metric-value">{resumo?.totalAlertas ?? 0}</span>
              </div>
              <div className="alertas-metric-box">
                <span className="alertas-metric-label">Severidade Máxima</span>
                <span className="alertas-metric-value">{renderSeveridade(resumo?.severidade || 'NORMAL')}</span>
              </div>
              <div className="alertas-metric-box">
                <span className="alertas-metric-label">Total Anomalias Activas</span>
                <span className="alertas-metric-value" style={{ color: '#ef4444' }}>
                  {anomalias.filter(a => a.status === 'PENDENTE' || a.status === 'EM_ANALISE' || a.status === 'EM_ESCALACAO').length}
                </span>
              </div>
            </div>

            {/* General summaries */}
            {sumario && (
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '1.25rem', marginBottom: '1.5rem' }}>
                <div style={{ padding: '1rem', background: '#f1f5f9', borderRadius: '8px' }}>
                  <span style={{ fontSize: '0.8rem', color: '#64748b', display: 'block' }}>PENDENTES</span>
                  <span style={{ fontSize: '1.4rem', fontWeight: 700 }}>{sumario.pendentes ?? 0}</span>
                </div>
                <div style={{ padding: '1rem', background: '#f1f5f9', borderRadius: '8px' }}>
                  <span style={{ fontSize: '0.8rem', color: '#64748b', display: 'block' }}>EM ANÁLISE OPERACIONAL</span>
                  <span style={{ fontSize: '1.4rem', fontWeight: 700, color: '#1d4ed8' }}>{sumario.emAnalise ?? 0}</span>
                </div>
                <div style={{ padding: '1rem', background: '#f1f5f9', borderRadius: '8px' }}>
                  <span style={{ fontSize: '0.8rem', color: '#64748b', display: 'block' }}>CRÍTICOS (Últimas 24h)</span>
                  <span style={{ fontSize: '1.4rem', fontWeight: 700, color: '#b91c1c' }}>{sumario.criticos24h ?? 0}</span>
                </div>
              </div>
            )}

            {/* Ingestion quarantine distribution */}
            {resumo?.porMotivo && Object.keys(resumo.porMotivo).length > 0 && (
              <div>
                <h3 style={{ marginBottom: '1rem', fontSize: '1.05rem', fontWeight: 700 }}>Erros de Validação Recentes (Quarentena)</h3>
                <div className="alertas-list">
                  {Object.entries(resumo.porMotivo).map(([motivo, count]) => (
                    <div key={motivo} className="alerta-item" style={{ padding: '0.8rem 1rem' }}>
                      <div className="alerta-info">
                        <span className="alerta-title" style={{ fontSize: '0.9rem' }}>{motivo}</span>
                        <span className="alerta-subtitle" style={{ fontSize: '0.8rem' }}>Rejeições no validador: {count}</span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </>
        ) : tab === 'anomalias' ? (
          <div>
            <h3 style={{ marginBottom: '1rem', fontSize: '1.05rem', fontWeight: 700 }}>Histórico Recente de Anomalias</h3>
            {anomalias.length === 0 ? (
              <div className="alertas-empty-state-container">
                <h4 className="empty-state-title">Sem anomalias registadas</h4>
                <p className="empty-state-description">A base de dados não contém registos de violações ou desvios de fraude.</p>
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                {anomalias.map(alerta => (
                  <div key={alerta.id} style={{ padding: '1.25rem', border: '1px solid #e2e8f0', borderRadius: '12px', background: '#fff', boxShadow: '0 1px 3px rgba(0,0,0,0.02)' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.5rem' }}>
                      <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                        <span style={{ fontSize: '0.8rem', background: '#f1f5f9', padding: '2px 6px', borderRadius: 4, fontWeight: 700 }}>ID: {alerta.id}</span>
                        <span style={{ fontWeight: 700, color: '#1e293b' }}>{alerta.type}</span>
                        {renderSeveridade(alerta.severity)}
                        {renderEstado(alerta.status)}
                      </div>
                      <span style={{ fontSize: '0.8rem', color: '#64748b' }}>
                        {alerta.createdAt ? new Date(alerta.createdAt).toLocaleString('pt-PT') : '—'}
                      </span>
                    </div>

                    <p style={{ fontSize: '0.925rem', color: '#334155', margin: '0.5rem 0' }}>{alerta.eventData}</p>

                    {alerta.ingestionHash && (
                      <div style={{ fontSize: '0.8rem', color: '#94a3b8', fontFamily: 'monospace' }}>Hash de Ingestão: {alerta.ingestionHash}</div>
                    )}

                    <div style={{ marginTop: '0.75rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.5rem' }}>
                      <span style={{ fontSize: '0.85rem', color: '#64748b' }}>
                        Atribuído a: <strong style={{ color: '#334155' }}>{alerta.assignedTo || 'Ninguém'}</strong>
                      </span>

                      {/* Actions */}
                      <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                        {alerta.status === 'PENDENTE' && (
                          <button onClick={() => handleAtribuir(alerta.id)} style={{ padding: '0.4rem 0.8rem', background: '#3b82f6', color: '#fff', border: 'none', borderRadius: 6, fontSize: '0.8rem', cursor: 'pointer', fontWeight: 600 }}>
                            Atribuir a mim
                          </button>
                        )}

                        {alerta.status === 'EM_ESCALACAO' && (
                          <button onClick={() => handleConfirmarEscalacao(alerta.id)} style={{ padding: '0.4rem 0.8rem', background: '#b91c1c', color: '#fff', border: 'none', borderRadius: 6, fontSize: '0.8rem', cursor: 'pointer', fontWeight: 700 }}>
                            Confirmar Escalação (CC)
                          </button>
                        )}

                        {(alerta.status === 'PENDENTE' || alerta.status === 'EM_ANALISE' || alerta.status === 'EM_ESCALACAO') && (
                          <>
                            <input 
                              type="text"
                              placeholder="Acção tomada..."
                              value={resolucaoTexto[alerta.id] || ''}
                              onChange={(e) => {
                                const val = e.target.value;
                                setResolucaoTexto(prev => ({ ...prev, [alerta.id]: val }));
                              }}
                              style={{ padding: '0.4rem 0.6rem', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: '0.8rem', outline: 'none' }}
                            />
                            <button onClick={() => handleResolver(alerta.id)} style={{ padding: '0.4rem 0.8rem', background: '#10b981', color: '#fff', border: 'none', borderRadius: 6, fontSize: '0.8rem', cursor: 'pointer', fontWeight: 600 }}>
                              Resolver
                            </button>
                            <button onClick={() => handleFalsoPositivo(alerta.id)} style={{ padding: '0.4rem 0.8rem', background: '#64748b', color: '#fff', border: 'none', borderRadius: 6, fontSize: '0.8rem', cursor: 'pointer', fontWeight: 600 }}>
                              Falso Positivo
                            </button>
                          </>
                        )}

                        {(alerta.status === 'RESOLVIDO' || alerta.status === 'FALSO_POSITIVO') && (
                          <div style={{ fontSize: '0.85rem', color: '#166534', background: '#f0fdf4', padding: '4px 10px', borderRadius: 6 }}>
                            Ação: <strong>{alerta.resolutionAction}</strong> {alerta.resolvedAt && `em ${new Date(alerta.resolvedAt).toLocaleTimeString('pt-PT')}`}
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        ) : (
          <div>
            <h3 style={{ marginBottom: '1rem', fontSize: '1.05rem', fontWeight: 700 }}>Mapa de Calor de Fiscalização</h3>
            <p style={{ fontSize: '0.9rem', color: '#64748b', marginBottom: '1.25rem' }}>
              Identificação visual de paragens e linhas críticas para planeamento estratégico de fiscalização de evasão tarifária.
            </p>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 300px', gap: '1.25rem', height: '400px' }}>
              {/* Map */}
              <div style={{ borderRadius: '12px', overflow: 'hidden', border: '1px solid #cbd5e1' }}>
                <MapContainer center={[41.5503, -8.4201]} zoom={13} style={{ height: '100%', width: '100%' }}>
                  <TileLayer
                    attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                    url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                  />
                  {hotspots.map(h => (
                    <Circle 
                      key={h.stopId}
                      center={[h.latitude, h.longitude]}
                      radius={150 + h.alertaCount * 12}
                      pathOptions={{
                        color: h.alertaCount > 15 ? '#ef4444' : '#f59e0b',
                        fillColor: h.alertaCount > 15 ? '#ef4444' : '#f59e0b',
                        fillOpacity: 0.35
                      }}
                    >
                      <Popup>
                        <div style={{ fontFamily: 'sans-serif' }}>
                          <h4 style={{ margin: '0 0 4px 0' }}>{h.name}</h4>
                          <p style={{ margin: 0, fontSize: '0.85rem' }}>Paragem: <strong>{h.stopId}</strong></p>
                          <p style={{ margin: 0, fontSize: '0.85rem' }}>Alertas: <strong style={{ color: '#ef4444' }}>{h.alertaCount}</strong></p>
                        </div>
                      </Popup>
                    </Circle>
                  ))}
                </MapContainer>
              </div>

              {/* Sidebar hotspots list */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', overflowY: 'auto', paddingRight: '4px' }}>
                <h4 style={{ margin: 0, fontSize: '0.9rem', fontWeight: 700, color: '#1e293b' }}>Zonas de Alto Risco</h4>
                {hotspots.map(h => (
                  <div key={h.stopId} style={{ padding: '0.75rem', border: '1px solid #e2e8f0', borderRadius: '8px', background: '#f8fafc' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 600, fontSize: '0.85rem' }}>
                      <span>{h.name}</span>
                      <span style={{ color: '#ef4444' }}>{h.alertaCount} alertas</span>
                    </div>
                    <div style={{ marginTop: '0.35rem', background: '#e2e8f0', height: '6px', borderRadius: '3px', overflow: 'hidden' }}>
                      <div style={{ background: h.alertaCount > 15 ? '#ef4444' : '#f59e0b', width: `${Math.min(100, (h.alertaCount / 20) * 100)}%`, height: '100%' }}></div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

export default PainelAlertas;