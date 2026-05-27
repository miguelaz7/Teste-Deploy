import React, { useState, useEffect, useCallback } from 'react';
import { getDadosAbertos, getExportacoes } from '../../logica_do_sistema/services/odService';
import { apiGet, apiPost } from '../../logica_do_sistema/services/apiClient';
import CustomDatePicker from '../analise/CustomDatePicker';
import './Od.css';

const API_BASE = process.env.REACT_APP_API_URL || 'http://localhost:8080';

function ExportacaoDados() {
  const [activeTab, setActiveTab] = useState('export'); // 'export' | 'dpo' | 'api'
  const [dadosExportados, setDadosExportados] = useState(null);
  const [pedidosExport, setPedidosExport] = useState([]);
  const [loading, setLoading] = useState(false);
  const [solicitando, setSolicitando] = useState(false);
  const [statusMessage, setStatusMessage] = useState(null);
  const [errorMessage, setErrorMessage] = useState(null);

  // Form Fields
  const [dataInicio, setDataInicio] = useState(new Date(Date.now() - 30 * 86400000).toISOString().split('T')[0]);
  const [dataFim, setDataFim] = useState(new Date().toISOString().split('T')[0]);
  const [formato, setFormato] = useState('CSV');
  const [routeId, setRouteId] = useState('');
  const [utilizador, setUtilizador] = useState('analista_dados');

  // API Call Simulator Fields
  const [apiToken, setApiToken] = useState('token_valido_ngsi_ld');
  const [apiType, setApiType] = useState('FareTransaction');
  const [apiResponse, setApiResponse] = useState(null);
  const [apiError, setApiError] = useState(null);

  const fetchPedidos = useCallback(async () => {
    try {
      const p = await getExportacoes();
      setPedidosExport(Array.isArray(p) ? p : []);
    } catch (err) {
      console.error("Erro a carregar pedidos", err);
    }
  }, []);

  useEffect(() => {
    fetchPedidos();
  }, [fetchPedidos]);

  const handleExportar = async (e) => {
    e.preventDefault();
    setLoading(true);
    setErrorMessage(null);
    setStatusMessage(null);
    setDadosExportados(null);

    try {
      // UC12.1 Call
      const res = await apiGet(
        `/api/exportacao/dados-abertos?dataInicio=${dataInicio}&dataFim=${dataFim}&formato=${formato}&routeId=${routeId}&utilizador=${utilizador}`
      );
      
      if (res && res.status === 'BLOCKED') {
        setErrorMessage(res.mensagem);
      } else {
        setDadosExportados(res);
        setStatusMessage("Dados processados com sucesso. Download disponível.");
      }
    } catch (err) {
      setErrorMessage("Exportação bloqueada pelo sistema. Detetados dados potencialmente identificáveis (FA1). O DPO foi notificado.");
    } finally {
      setLoading(false);
    }
  };

  const handleSubmeterAprovacaoDPO = async () => {
    setSolicitando(true);
    setErrorMessage(null);
    setStatusMessage(null);
    try {
      const res = await fetch(`${API_BASE}/api/ngsi-ld/exportacoes`, {
        method: 'POST',
        headers: { 
          'Content-Type': 'application/json',
          'X-Api-User': utilizador 
        },
        body: JSON.stringify({
          formato,
          periodoInicio: dataInicio,
          periodoFim: dataFim,
          filtros: routeId ? `routeId=${routeId}` : 'sem_filtros'
        })
      });
      const data = await res.json();
      setStatusMessage(`Pedido submetido com sucesso! ID: ${data.id}. Estado: ${data.estado}`);
      await fetchPedidos();
    } catch (err) {
      setErrorMessage("Erro ao submeter pedido ao DPO.");
    } finally {
      setSolicitando(false);
    }
  };

  const handleDecidirDPO = async (id, decisao) => {
    try {
      const res = await fetch(`${API_BASE}/api/ngsi-ld/exportacoes/${id}/aprovar`, {
        method: 'PUT',
        headers: { 
          'Content-Type': 'application/json',
          'X-Api-User': 'dpo_oficial' 
        },
        body: JSON.stringify({ decisao })
      });
      if (!res.ok) throw new Error();
      alert(`Exportação ${decisao.toLowerCase()} com sucesso!`);
      await fetchPedidos();
    } catch (err) {
      alert("Falha ao registar decisão do DPO.");
    }
  };

  // Simular chamada de API externa via cliente NGSI-LD (UC12.2)
  const handleSimularApiCall = async () => {
    setApiResponse(null);
    setApiError(null);
    try {
      const res = await fetch(`${API_BASE}/api/ngsi-ld/entities?type=${apiType}`, {
        headers: { 'Authorization': `Bearer ${apiToken}` }
      });
      if (!res.ok) throw new Error();
      const data = await res.json();
      setApiResponse(data);
    } catch (err) {
      setApiError("Erro: Credenciais inválidas (FA2) ou Erro de validação de esquema (FA3).");
    }
  };

  const handleDownloadFile = () => {
    if (!dadosExportados) return;
    let content = '';
    let fileName = `export_${dataInicio}_to_${dataFim}.${formato.toLowerCase()}`;

    if (formato === 'CSV') {
      const regs = dadosExportados.registos || [];
      if (regs.length > 0) {
        const headers = Object.keys(regs[0]);
        content = [
          headers.join(','),
          ...regs.map(r => headers.map(h => `"${String(r[h] ?? '').replace(/"/g, '""')}"`).join(','))
        ].join('\n');
      } else {
        content = "sem_registos";
      }
    } else {
      content = JSON.stringify(dadosExportados, null, 2);
    }

    const blob = new Blob([content], { type: 'text/plain;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
  };

  return (
    <div className="analise-container">
      <div className="analise-card">
        {/* Menu Superior de Interoperabilidade */}
        <div className="analise-tabs" style={{ display: 'flex', gap: '1rem', borderBottom: '2px solid #e2e8f0', marginBottom: '1.5rem' }}>
          <button 
            onClick={() => setActiveTab('export')}
            className={`analise-tab ${activeTab === 'export' ? 'active' : ''}`}
          >
            Exportar Dados Abertos
          </button>
          <button 
            onClick={() => setActiveTab('dpo')}
            className={`analise-tab ${activeTab === 'dpo' ? 'active' : ''}`}
          >
            Aprovações DPO
          </button>
          <button 
            onClick={() => setActiveTab('api')}
            className={`analise-tab ${activeTab === 'api' ? 'active' : ''}`}
          >
            API NGSI-LD Integrador
          </button>
        </div>

        {activeTab === 'export' && (
          <div className="od-card-body" style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
            {/* Cartão de exportação */}
            <div>
              <div className="od-card-header" style={{ borderBottom: 'none', paddingBottom: 0, marginBottom: '0.75rem' }}>
                <h2 style={{ fontSize: '1.15rem', color: '#0f172a', display: 'flex', alignItems: 'center', gap: '0.5rem', borderLeft: '4px solid #3b82f6', paddingLeft: '10px', textTransform: 'uppercase', letterSpacing: '-0.2px', fontWeight: 800 }}>
                  Configuração do Dataset Aprovado
                </h2>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                <form onSubmit={handleExportar} style={{ display: 'flex', gap: '1.5rem', padding: '1.25rem', backgroundColor: '#f8fafc', borderRadius: '12px', border: '1px solid #e2e8f0', boxSizing: 'border-box', alignItems: 'flex-end', width: '100%' }}>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', flex: 1 }}>
                    <CustomDatePicker label="Data Início" value={dataInicio} onChange={setDataInicio} />
                  </div>

                  <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', flex: 1 }}>
                    <CustomDatePicker label="Data Fim" value={dataFim} onChange={setDataFim} />
                  </div>

                  <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', flex: 1 }}>
                    <label style={{ fontSize: '0.75rem', fontWeight: 700, color: '#475569', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Formato</label>
                    <select 
                      style={{ padding: '0.45rem 0.85rem', border: '1px solid #cbd5e1', borderRadius: '8px', backgroundColor: '#ffffff', width: '100%', height: '38px', fontSize: '0.9rem', color: '#0f172a', fontWeight: '500', boxSizing: 'border-box' }}
                      value={formato}
                      onChange={(e) => setFormato(e.target.value)}
                    >
                      <option value="CSV">CSV (Formato Aberto)</option>
                      <option value="EXCEL">Excel (Múltiplas Sheets)</option>
                      <option value="JSON">JSON (NGSI-LD)</option>
                      <option value="GEOJSON">GeoJSON (Descaracterizado)</option>
                    </select>
                  </div>

                  <button type="submit" className="btn-download" style={{ height: '38px', flexShrink: 0 }} disabled={loading}>
                    {loading ? 'A processar...' : 'Exportar Dataset'}
                  </button>
                </form>

                <div style={{ display: 'flex', gap: '10px' }}>
                  <button 
                    onClick={handleSubmeterAprovacaoDPO} 
                    disabled={solicitando} 
                    style={{ 
                      height: '38px',
                      padding: '0 1.25rem', 
                      background: 'linear-gradient(135deg, #0d9488, #0f766e)', 
                      color: '#ffffff', 
                      border: 'none', 
                      borderRadius: '8px', 
                      fontWeight: 700, 
                      fontSize: '0.85rem',
                      cursor: solicitando ? 'not-allowed' : 'pointer',
                      transition: 'all 0.2s ease',
                      boxShadow: '0 4px 12px rgba(13, 148, 136, 0.15)'
                    }}
                  >
                    {solicitando ? 'A submeter...' : 'Solicitar Aprovação Oficial ao DPO'}
                  </button>
                </div>

                {errorMessage && (
                  <div style={{ padding: '1rem', backgroundColor: '#fee2e2', color: '#b91c1c', borderRadius: '8px', border: '1px solid #fecaca', marginTop: '0.5rem', fontWeight: '500', fontSize: '0.9rem' }}>
                    <strong>🔒 Alerta de Segurança:</strong> {errorMessage}
                  </div>
                )}

                {statusMessage && (
                  <div style={{ padding: '1rem', backgroundColor: '#ecfdf5', color: '#065f46', borderRadius: '8px', border: '1px solid #a7f3d0', marginTop: '0.5rem', fontWeight: '500', fontSize: '0.9rem' }}>
                    <strong>✓ Sucesso:</strong> {statusMessage}
                  </div>
                )}
              </div>
            </div>

            {/* Resultado das Exportações geradas */}
            {dadosExportados && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', marginTop: '1rem' }}>
                <div className="od-card-header" style={{ borderBottom: 'none', paddingBottom: 0, marginBottom: '0.5rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <h2 style={{ fontSize: '1.15rem', color: '#0f172a', display: 'flex', alignItems: 'center', gap: '0.5rem', borderLeft: '4px solid #3b82f6', paddingLeft: '10px', textTransform: 'uppercase', letterSpacing: '-0.2px', fontWeight: 800 }}>
                    Dataset Processado
                  </h2>
                  <button className="btn-resolver" onClick={handleDownloadFile}>
                    Descarregar Ficheiro ({formato})
                  </button>
                </div>
                
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                  <div style={{ background: '#f8fafc', padding: '1.25rem', borderRadius: '12px', border: '1px solid #e2e8f0' }}>
                    <h4 style={{ margin: '0 0 0.5rem 0', color: '#1e3a8a', fontSize: '0.95rem', fontWeight: 800 }}>Metadados do Ficheiro</h4>
                    <p style={{ margin: 0, fontSize: '0.85rem', color: '#475569', fontWeight: 500 }}><strong>Disclaimer:</strong> {dadosExportados.metadados?.disclaimer}</p>
                    <p style={{ margin: '6px 0 0 0', fontSize: '0.85rem', color: '#475569', fontWeight: 500 }}><strong>Checksum (SHA-256):</strong> <code style={{ backgroundColor: '#e2e8f0', padding: '2px 6px', borderRadius: '4px', fontFamily: 'monospace' }}>{dadosExportados.checksum}</code></p>
                  </div>

                  <div className="od-table-container">
                    <table className="od-table">
                      <thead>
                        <tr>
                          <th>ID Entidade</th>
                          <th>Modelo/Tipo</th>
                          <th>Partição</th>
                        </tr>
                      </thead>
                      <tbody>
                        {(dadosExportados.registos || dadosExportados.sheets?.dados || []).slice(0, 10).map((r, i) => (
                          <tr key={i}>
                            <td style={{ color: '#0f172a', fontWeight: 700 }}>{r.entityId}</td>
                            <td>{r.entityType}</td>
                            <td>{r.partitionDate}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            )}
          </div>
        )}

        {activeTab === 'dpo' && (
          <div className="od-card-body" style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
            <div className="od-card-header" style={{ borderBottom: 'none', paddingBottom: 0, marginBottom: '0.5rem' }}>
              <h2 style={{ fontSize: '1.15rem', color: '#0f172a', display: 'flex', alignItems: 'center', gap: '0.5rem', borderLeft: '4px solid #3b82f6', paddingLeft: '10px', textTransform: 'uppercase', letterSpacing: '-0.2px', fontWeight: 800 }}>
                Pedidos de Exportação pendentes do DPO
              </h2>
            </div>
            
            {pedidosExport.length === 0 ? (
              <div className="od-empty-state">Sem pedidos registados para o DPO.</div>
            ) : (
              <div className="od-table-container">
                <table className="od-table">
                  <thead>
                    <tr>
                      <th>ID Pedido</th>
                      <th>Solicitado por</th>
                      <th>Período</th>
                      <th>Formato</th>
                      <th>Registos</th>
                      <th>Estado</th>
                      <th>Decisão</th>
                    </tr>
                  </thead>
                  <tbody>
                    {pedidosExport.map((row, idx) => (
                      <tr key={idx}>
                        <td style={{ color: '#0f172a', fontWeight: 700 }}>#{row.id}</td>
                        <td style={{ color: '#0f172a', fontWeight: 600 }}>{row.requestedBy}</td>
                        <td>{row.periodStart} → {row.periodEnd}</td>
                        <td>{row.format}</td>
                        <td style={{ color: '#0f172a', fontWeight: 700 }}>{row.totalRecords}</td>
                        <td>
                          <span className={`badge-estado ${row.status === 'EXPORTADA' ? 'estado-sucesso' : row.status === 'REJEITADA' ? 'estado-falha' : 'estado-pendente'}`}>
                            {row.status}
                          </span>
                        </td>
                        <td>
                          {row.status === 'PENDENTE_DPO' && (
                            <div style={{ display: 'flex', gap: '6px' }}>
                              <button 
                                onClick={() => handleDecidirDPO(row.id, 'APROVADA')}
                                style={{ padding: '4px 10px', background: '#16a34a', color: '#fff', border: 'none', borderRadius: '6px', cursor: 'pointer', fontSize: '0.75rem', fontWeight: 700 }}
                              >
                                Aprovar
                              </button>
                              <button 
                                onClick={() => handleDecidirDPO(row.id, 'REJEITADA')}
                                style={{ padding: '4px 10px', background: '#dc2626', color: '#fff', border: 'none', borderRadius: '6px', cursor: 'pointer', fontSize: '0.75rem', fontWeight: 700 }}
                              >
                                Rejeitar
                              </button>
                            </div>
                          )}
                          {row.status !== 'PENDENTE_DPO' && (
                            <span style={{ fontSize: '0.8rem', color: '#64748b', fontWeight: 600 }}>Revisado por {row.approvedBy || 'DPO'}</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}

        {activeTab === 'api' && (
          <div className="od-card-body" style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
            <div className="od-card-header" style={{ borderBottom: 'none', paddingBottom: 0, marginBottom: '0.5rem' }}>
              <h2 style={{ fontSize: '1.15rem', color: '#0f172a', display: 'flex', alignItems: 'center', gap: '0.5rem', borderLeft: '4px solid #3b82f6', paddingLeft: '10px', textTransform: 'uppercase', letterSpacing: '-0.2px', fontWeight: 800 }}>
                Simulador de Cliente API NGSI-LD
              </h2>
            </div>
            
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
              <div style={{ display: 'flex', gap: '1.5rem', padding: '1.25rem', backgroundColor: '#f8fafc', borderRadius: '12px', border: '1px solid #e2e8f0', boxSizing: 'border-box', alignItems: 'flex-end', width: '100%' }}>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', flex: 1.5 }}>
                  <label style={{ fontSize: '0.75rem', fontWeight: 700, color: '#475569', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Chave OAuth2 / Token</label>
                  <input 
                    type="text" 
                    style={{ padding: '0.45rem 0.85rem', border: '1px solid #cbd5e1', borderRadius: '8px', backgroundColor: '#ffffff', width: '100%', height: '38px', fontSize: '0.9rem', color: '#0f172a', fontWeight: '500', boxSizing: 'border-box' }}
                    value={apiToken}
                    onChange={(e) => setApiToken(e.target.value)}
                    placeholder="Bearer ..."
                  />
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', flex: 1 }}>
                  <label style={{ fontSize: '0.75rem', fontWeight: 700, color: '#475569', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Entity Type</label>
                  <select 
                    style={{ padding: '0.45rem 0.85rem', border: '1px solid #cbd5e1', borderRadius: '8px', backgroundColor: '#ffffff', width: '100%', height: '38px', fontSize: '0.9rem', color: '#0f172a', fontWeight: '500', boxSizing: 'border-box' }}
                    value={apiType}
                    onChange={(e) => setApiType(e.target.value)}
                  >
                    <option value="FareTransaction">FareTransaction (Smart Data Models)</option>
                    <option value="PublicTransportRoute">PublicTransportRoute</option>
                    <option value="PublicTransportStop">PublicTransportStop</option>
                    <option value="InvalidType">InvalidType</option>
                  </select>
                </div>

                <button className="btn-download" onClick={handleSimularApiCall} style={{ height: '38px', flexShrink: 0 }}>
                  Fazer Pedido à API
                </button>
              </div>

              {apiError && (
                <div style={{ padding: '1rem', backgroundColor: '#fee2e2', color: '#b91c1c', borderRadius: '8px', border: '1px solid #fecaca', fontWeight: '500', fontSize: '0.9rem' }}>
                  {apiError}
                </div>
              )}

              {apiResponse && (
                <div style={{ background: '#0f172a', color: '#38bdf8', padding: '1.25rem', borderRadius: '12px', fontFamily: 'monospace', fontSize: '0.85rem', maxHeight: '300px', overflowY: 'auto', border: '1px solid #1e293b', boxShadow: 'inset 0 2px 8px rgba(0,0,0,0.5)' }}>
                  <pre style={{ margin: 0 }}>{JSON.stringify(apiResponse, null, 2)}</pre>
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

export default ExportacaoDados;