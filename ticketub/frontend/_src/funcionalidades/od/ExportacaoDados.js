import React, { useState, useEffect, useCallback } from 'react';
import { getDadosAbertos, getExportacoes } from '../../logica_do_sistema/services/odService';
import { apiGet, apiPost } from '../../logica_do_sistema/services/apiClient';
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
    <div className="planeamento-container">
      {/* Menu Superior de Interoperabilidade */}
      <div style={{ display: 'flex', gap: '8px', marginBottom: '1.25rem', borderBottom: '1px solid #e2e8f0', paddingBottom: '0.5rem' }}>
        <button 
          onClick={() => setActiveTab('export')}
          className={`btn-primary ${activeTab === 'export' ? '' : 'inactive-tab-btn'}`}
          style={{ background: activeTab === 'export' ? '#2563eb' : '#94a3b8', border: 'none' }}
        >
          Exportar Dados Abertos (UC12.1)
        </button>
        <button 
          onClick={() => setActiveTab('dpo')}
          className={`btn-primary ${activeTab === 'dpo' ? '' : 'inactive-tab-btn'}`}
          style={{ background: activeTab === 'dpo' ? '#2563eb' : '#94a3b8', border: 'none' }}
        >
          Aprovações DPO (UC12.1 / UC12.3)
        </button>
        <button 
          onClick={() => setActiveTab('api')}
          className={`btn-primary ${activeTab === 'api' ? '' : 'inactive-tab-btn'}`}
          style={{ background: activeTab === 'api' ? '#2563eb' : '#94a3b8', border: 'none' }}
        >
          API NGSI-LD Integrador (UC12.2)
        </button>
      </div>

      {activeTab === 'export' && (
        <>
          {/* Cartão de exportação */}
          <div className="planeamento-card">
            <div className="planeamento-card-header">
              <h2>Configuração do Dataset Aprovado (UC12.1)</h2>
            </div>
            <div className="planeamento-card-body">
              <form onSubmit={handleExportar} style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '1rem', alignItems: 'flex-end' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: '#475569', marginBottom: '6px' }}>
                    Data Início:
                  </label>
                  <input 
                    type="date"
                    style={{ width: '100%', padding: '0.5rem', border: '1px solid #cbd5e1', borderRadius: '8px' }}
                    value={dataInicio}
                    onChange={(e) => setDataInicio(e.target.value)}
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: '#475569', marginBottom: '6px' }}>
                    Data Fim:
                  </label>
                  <input 
                    type="date"
                    style={{ width: '100%', padding: '0.5rem', border: '1px solid #cbd5e1', borderRadius: '8px' }}
                    value={dataFim}
                    onChange={(e) => setDataFim(e.target.value)}
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: '#475569', marginBottom: '6px' }}>
                    Formato:
                  </label>
                  <select 
                    style={{ width: '100%', padding: '0.5rem', border: '1px solid #cbd5e1', borderRadius: '8px', backgroundColor: '#fff' }}
                    value={formato}
                    onChange={(e) => setFormato(e.target.value)}
                  >
                    <option value="CSV">CSV (Formato Aberto)</option>
                    <option value="EXCEL">Excel (Múltiplas Sheets)</option>
                    <option value="JSON">JSON (NGSI-LD)</option>
                    <option value="GEOJSON">GeoJSON (Descaracterizado)</option>
                  </select>
                </div>

                <div>
                  <button type="submit" className="btn-primary" style={{ width: '100%', padding: '0.55rem' }} disabled={loading}>
                    {loading ? 'A processar...' : 'Exportar Dataset'}
                  </button>
                </div>
              </form>

              <div style={{ marginTop: '1rem', display: 'flex', gap: '10px' }}>
                <button className="btn-primary" onClick={handleSubmeterAprovacaoDPO} disabled={solicitando} style={{ background: '#0f766e' }}>
                  {solicitando ? 'A submeter...' : 'Solicitar Aprovação Oficial ao DPO'}
                </button>
              </div>

              {/* Status / Error feedback */}
              {errorMessage && (
                <div style={{ padding: '1rem', backgroundColor: '#fee2e2', color: '#b91c1c', borderRadius: '8px', border: '1px solid #fecaca', marginTop: '1rem' }}>
                  <strong>🔒 Alerta de Segurança (FA1):</strong> {errorMessage}
                </div>
              )}

              {statusMessage && (
                <div style={{ padding: '1rem', backgroundColor: '#ecfdf5', color: '#065f46', borderRadius: '8px', border: '1px solid #a7f3d0', marginTop: '1rem' }}>
                  <strong>✓ Sucesso:</strong> {statusMessage}
                </div>
              )}
            </div>
          </div>

          {/* Resultado das Exportações geradas */}
          {dadosExportados && (
            <div className="planeamento-card" style={{ marginTop: '1.5rem' }}>
              <div className="planeamento-card-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <h2>Dataset Processado</h2>
                <button className="btn-primary" onClick={handleDownloadFile} style={{ background: '#16a34a' }}>
                  Descarregar Ficheiro ({formato})
                </button>
              </div>
              <div className="planeamento-card-body">
                <div style={{ background: '#f8fafc', padding: '1rem', borderRadius: '8px', border: '1px solid #e2e8f0', marginBottom: '1rem' }}>
                  <h4 style={{ margin: '0 0 0.5rem 0', color: '#1e3a8a' }}>Metadados do Ficheiro</h4>
                  <p style={{ margin: 0, fontSize: '0.85rem' }}><strong>Disclaimer:</strong> {dadosExportados.metadados?.disclaimer}</p>
                  <p style={{ margin: '4px 0 0 0', fontSize: '0.85rem' }}><strong>Checksum (SHA-256):</strong> <code>{dadosExportados.checksum}</code></p>
                </div>

                <div className="planeamento-table-container">
                  <table className="planeamento-table">
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
                          <td>{r.entityId}</td>
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
        </>
      )}

      {activeTab === 'dpo' && (
        <div className="planeamento-card">
          <div className="planeamento-card-header">
            <h2>Pedidos de Exportação pendentes do DPO (UC12.3)</h2>
          </div>
          <div className="planeamento-card-body">
            {pedidosExport.length === 0 ? (
              <div className="planeamento-empty-state">Sem pedidos registados para o DPO.</div>
            ) : (
              <div className="planeamento-table-container">
                <table className="planeamento-table">
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
                        <td>#{row.id}</td>
                        <td>{row.requestedBy}</td>
                        <td>{row.periodStart} → {row.periodEnd}</td>
                        <td>{row.format}</td>
                        <td>{row.totalRecords}</td>
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
                                style={{ padding: '2px 8px', background: '#16a34a', color: '#fff', border: 'none', borderRadius: '4px', cursor: 'pointer', fontSize: '0.75rem' }}
                              >
                                Aprovar
                              </button>
                              <button 
                                onClick={() => handleDecidirDPO(row.id, 'REJEITADA')}
                                style={{ padding: '2px 8px', background: '#dc2626', color: '#fff', border: 'none', borderRadius: '4px', cursor: 'pointer', fontSize: '0.75rem' }}
                              >
                                Rejeitar
                              </button>
                            </div>
                          )}
                          {row.status !== 'PENDENTE_DPO' && (
                            <span style={{ fontSize: '0.8rem', color: '#64748b' }}>Revisado por {row.approvedBy || 'DPO'}</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}

      {activeTab === 'api' && (
        <div className="planeamento-card">
          <div className="planeamento-card-header">
            <h2>Simulador de Cliente API NGSI-LD (UC12.2)</h2>
          </div>
          <div className="planeamento-card-body">
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '1rem', marginBottom: '1.5rem', alignItems: 'flex-end' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: '#475569', marginBottom: '6px' }}>
                  Chave OAuth2/Token (Authorization Bearer):
                </label>
                <input 
                  type="text" 
                  style={{ width: '100%', padding: '0.5rem', border: '1px solid #cbd5e1', borderRadius: '8px' }}
                  value={apiToken}
                  onChange={(e) => setApiToken(e.target.value)}
                  placeholder="Bearer ..."
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: '#475569', marginBottom: '6px' }}>
                  Smart Data Model / Entity Type:
                </label>
                <select 
                  style={{ width: '100%', padding: '0.5rem', border: '1px solid #cbd5e1', borderRadius: '8px', backgroundColor: '#fff' }}
                  value={apiType}
                  onChange={(e) => setApiType(e.target.value)}
                >
                  <option value="FareTransaction">FareTransaction (Smart Data Models)</option>
                  <option value="PublicTransportRoute">PublicTransportRoute</option>
                  <option value="PublicTransportStop">PublicTransportStop</option>
                  <option value="InvalidType">InvalidType (Gera Erro FA3)</option>
                </select>
              </div>

              <div>
                <button className="btn-primary" onClick={handleSimularApiCall} style={{ width: '100%', padding: '0.55rem' }}>
                  Fazer Pedido à API
                </button>
              </div>
            </div>

            {apiError && (
              <div style={{ padding: '1rem', backgroundColor: '#fee2e2', color: '#b91c1c', borderRadius: '8px', border: '1px solid #fecaca', marginBottom: '1rem' }}>
                {apiError}
              </div>
            )}

            {apiResponse && (
              <div style={{ background: '#0f172a', color: '#38bdf8', padding: '1rem', borderRadius: '8px', fontFamily: 'monospace', fontSize: '0.85rem', maxHeight: '300px', overflowY: 'auto' }}>
                <pre>{JSON.stringify(apiResponse, null, 2)}</pre>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

export default ExportacaoDados;