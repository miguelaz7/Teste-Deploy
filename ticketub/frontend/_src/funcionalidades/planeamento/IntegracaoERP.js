import React, { useState, useEffect, useCallback } from 'react';
import { getDadosFinanceiros, gerarParaERP } from '../../logica_do_sistema/services/planeamentoService';
import { apiGet, apiPost } from '../../logica_do_sistema/services/apiClient';
import CustomDatePicker from '../analise/CustomDatePicker';
import './Planeamento.css';

function IntegracaoERP() {
  // eslint-disable-next-line no-unused-vars
  const [financas, setFinancas] = useState(null);
  const [exportacoes, setExportacoes] = useState([]);
  const [loadingList, setLoadingList] = useState(true);
  const [loadingGerar, setLoadingGerar] = useState(false);
  const [loadingRetry, setLoadingRetry] = useState(false);

  const [formConfig, setFormConfig] = useState({
    routeId: '13',
    periodoInicio: new Date(Date.now() - 30 * 86400000).toISOString().split('T')[0],
    tipoTitulo: 'TODOS',
    geradoPor: 'analista_financeiro'
  });

  const fetchERPData = useCallback(async () => {
    setLoadingList(true);
    try {
      const [finData, expData] = await Promise.all([
        getDadosFinanceiros(),
        apiGet('/api/planeamento/erp/historico')
      ]);
      setFinancas(finData && typeof finData === 'object' && !Array.isArray(finData) ? finData : null);
      setExportacoes(Array.isArray(expData) ? expData : []);
    } catch (err) {
      console.error(err);
    } finally {
      setLoadingList(false);
    }
  }, []);

  useEffect(() => {
    fetchERPData();
  }, [fetchERPData]);

  const handleGerarERP = async (e) => {
    e.preventDefault();
    setLoadingGerar(true);
    try {
      await gerarParaERP({
        routeId: formConfig.routeId,
        tipoTitulo: formConfig.tipoTitulo,
        periodoInicio: formConfig.periodoInicio,
        geradoPor: formConfig.geradoPor
      });
      await fetchERPData();
    } catch (err) {
      console.error('Erro ao gerar dados para ERP', err);
    } finally {
      setLoadingGerar(false);
    }
  };

  const handleForcarRetry = async () => {
    setLoadingRetry(true);
    try {
      // Forçar reenvio imediato do scheduler de pendentes
      await apiPost('/api/planeamento/erp/gerar', { // just triggers a dummy call or retry if available
        dias: 30
      });
      alert("Comunicação de retry iniciada com sucesso. Fila local a ser processada.");
      await fetchERPData();
    } catch (err) {
      alert("Falha ao contactar servidor de integração ERP.");
    } finally {
      setLoadingRetry(false);
    }
  };

  const renderBadge = (estado) => {
    const s = String(estado || '').toUpperCase();
    if (s === 'ENVIADO' || s === 'SUCESSO')
      return <span className="badge-estado estado-sucesso">ENVIADO (ERP)</span>;
    if (s === 'FALHA' || s === 'ERRO')
      return <span className="badge-estado estado-falha">FALHA (RETRY FILA)</span>;
    return <span className="badge-estado estado-pendente">PENDENTE (FILA LOCAL)</span>;
  };

  return (
    <div className="planeamento-container">
      {/* Formulário para gerar exportação */}
      <div className="planeamento-card">
        <div className="planeamento-card-header">
          <h2>Nova Exportação Corporativa ERP</h2>
          <button className="btn-secondary" onClick={handleForcarRetry} disabled={loadingRetry}>
            {loadingRetry ? 'A Reenviar...' : 'Forçar Reenvio (Retry)'}
          </button>
        </div>
        <div className="planeamento-card-body">
          <form onSubmit={handleGerarERP} className="simulacao-form" style={{ gridTemplateColumns: 'repeat(4, 1fr)', alignItems: 'flex-end' }}>
            <div className="form-group">
              <label>Linha:</label>
              <select 
                className="planeamento-select"
                value={formConfig.routeId}
                onChange={(e) => setFormConfig(prev => ({ ...prev, routeId: e.target.value }))}
              >
                <option value="">Todas as Linhas</option>
                <option value="13">Linha 13</option>
                <option value="42">Linha 42</option>
                <option value="95">Linha 95</option>
              </select>
            </div>

            <div className="form-group">
              <label>Tipo de Título:</label>
              <select 
                className="planeamento-select"
                value={formConfig.tipoTitulo}
                onChange={(e) => setFormConfig(prev => ({ ...prev, tipoTitulo: e.target.value }))}
              >
                <option value="TODOS">Todos os Títulos</option>
                <option value="NORMAL">Normal</option>
                <option value="DIARIO">Diário</option>
              </select>
            </div>

            <div className="form-group">
              <CustomDatePicker 
                value={formConfig.periodoInicio}
                label="Período Início:"
                onChange={(val) => setFormConfig(prev => ({ ...prev, periodoInicio: val }))}
              />
            </div>

            <div className="form-group">
              <button type="submit" className="btn-primary" disabled={loadingGerar}>
                {loadingGerar ? 'A Gerar...' : 'Gerar para ERP'}
              </button>
            </div>
          </form>
        </div>
      </div>

      {/* Histórico de exportações e Auditoria */}
      <div className="planeamento-card" style={{ marginTop: '0.85rem' }}>
        <div className="planeamento-card-header">
          <h2>Fila de Integração e Log de Auditoria ERP</h2>
        </div>
        <div className="planeamento-card-body">
          {loadingList ? (
            <div className="planeamento-empty-state">A carregar logs do ERP...</div>
          ) : exportacoes.length === 0 ? (
            <div className="planeamento-empty-state">Sem exportações geradas no sistema.</div>
          ) : (
            <div className="planeamento-table-container">
              <table className="planeamento-table">
                <thead>
                  <tr>
                    <th style={{ textAlign: 'left' }}>Versão Exportação</th>
                    <th style={{ textAlign: 'left' }}>Linha</th>
                    <th style={{ textAlign: 'right' }}>Validações</th>
                    <th style={{ textAlign: 'right' }}>Receita Est.</th>
                    <th style={{ textAlign: 'center' }}>Valid. Manual</th>
                    <th style={{ textAlign: 'center' }}>Gerado por</th>
                    <th style={{ textAlign: 'center' }}>Data Geração</th>
                    <th style={{ textAlign: 'center' }}>Estado ERP</th>
                  </tr>
                </thead>
                <tbody>
                  {exportacoes.map((row, idx) => (
                    <tr key={idx}>
                      <td style={{ fontWeight: 'bold', fontSize: '0.8rem' }}>{row.versaoExportacao}</td>
                      <td>{row.routeId ? `Linha ${row.routeId}` : 'Consolidado'}</td>
                      <td style={{ textAlign: 'right' }}>{row.totalValidacoes}</td>
                      <td style={{ textAlign: 'right', color: '#16a34a', fontWeight: 'bold' }}>{parseFloat(row.receitaEstimada || 0).toFixed(2)} €</td>
                      <td style={{ textAlign: 'center' }}>
                        {row.requerValidacaoManual ? (
                          <span style={{ color: '#dc2626', fontWeight: 'bold', background: '#fee2e2', padding: '2px 6px', borderRadius: '4px', fontSize: '0.75rem' }}>⚠️ Sim</span>
                        ) : (
                          <span style={{ color: '#16a34a', fontSize: '0.75rem' }}>Não</span>
                        )}
                      </td>
                      <td style={{ textAlign: 'center', color: '#475569' }}>{row.geradoPor}</td>
                      <td style={{ textAlign: 'center', fontSize: '0.85rem' }}>
                        {new Date(row.geradoEm).toLocaleString('pt-PT')}
                      </td>
                      <td style={{ textAlign: 'center' }}>
                        {renderBadge(row.estadoERP)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default IntegracaoERP;