import React, { useState, useEffect, useCallback } from 'react';
import { submeterSimulacao, getCenarios } from '../../logica_do_sistema/services/planeamentoService';
import { apiPost } from '../../logica_do_sistema/services/apiClient';
import './Planeamento.css';

function SimulacaoCenarios() {
  const [cenarios, setCenarios] = useState([]);
  const [projecao, setProjecao] = useState(null);
  const [loadingList, setLoadingList] = useState(true);
  const [loadingSim, setLoadingSim] = useState(false);
  const [errorMessage, setErrorMessage] = useState(null);

  const [formData, setFormData] = useState({
    routeId: '',
    descricao: '',
    valorAntes: '10',
    valorDepois: '8',
    periodo: 'DIA_COMPLETO',
    criadoPor: 'gestor_planeamento'
  });

  const fetchCenarios = useCallback(async () => {
    setLoadingList(true);
    try {
      const data = await getCenarios();
      setCenarios(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error(err);
      setCenarios([]);
    } finally {
      setLoadingList(false);
    }
  }, []);

  useEffect(() => {
    fetchCenarios();
  }, [fetchCenarios]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleSimular = async (e) => {
    e.preventDefault();
    setErrorMessage(null);
    setProjecao(null);

    // Frontend pre-check for FA3
    if (!formData.routeId || parseFloat(formData.valorAntes) <= 0 || parseFloat(formData.valorDepois) <= 0) {
      setErrorMessage("Parâmetros inválidos: Frequências devem ser superiores a 0.");
      return;
    }

    setLoadingSim(true);
    try {
      const res = await submeterSimulacao({
        routeId: formData.routeId,
        descricao: formData.descricao || "Ajuste de frequências",
        periodo: formData.periodo,
        valorAntes: parseFloat(formData.valorAntes),
        valorDepois: parseFloat(formData.valorDepois),
        criadoPor: formData.criadoPor
      });

      if (res.status === 'ERRO') {
        setErrorMessage(res.mensagem);
      } else {
        setProjecao(res);
        await fetchCenarios();
      }
    } catch (err) {
      setErrorMessage("Erro ao ligar ao simulador. Por favor, valide os parâmetros inseridos.");
    } finally {
      setLoadingSim(false);
    }
  };

  const handleUpdateStatus = async (codigo, novoEstado) => {
    try {
      await apiPost(`/api/planeamento/cenarios/${codigo}/status`, {
        estado: novoEstado,
        utilizador: formData.criadoPor
      });
      await fetchCenarios();
    } catch (err) {
      alert("Falha ao atualizar estado do cenário.");
    }
  };

  const getStatusBadgeClass = (status) => {
    const s = String(status || '').toUpperCase();
    if (s === 'APPROVED' || s === 'APROVADO') return 'badge-estado estado-sucesso';
    if (s === 'REJECTED' || s === 'REJEITADO') return 'badge-estado estado-falha';
    if (s === 'IMPLEMENTED') return 'badge-estado estado-implementado';
    return 'badge-estado estado-pendente';
  };

  return (
    <div className="planeamento-container">
      {/* Formulário de Simulação */}
      <div className="planeamento-card">
        <div className="planeamento-card-header">
          <h2>
            Nova Simulação de Cenário
          </h2>
        </div>
        <div className="planeamento-card-body">
          <form className="simulacao-form" onSubmit={handleSimular}>
            <div className="form-group">
              <label>Linha (Route ID):</label>
              <input 
                type="text" 
                name="routeId" 
                className="planeamento-input"
                value={formData.routeId} 
                onChange={handleChange} 
                placeholder="Ex: 13, 42, 95"
                required 
              />
            </div>

            <div className="form-group">
              <label>Frequência Atual (circulações/hora):</label>
              <input 
                type="number" 
                name="valorAntes" 
                className="planeamento-input"
                value={formData.valorAntes} 
                onChange={handleChange} 
                required 
              />
            </div>

            <div className="form-group">
              <label>Frequência Projetada (circulações/hora):</label>
              <input 
                type="number" 
                name="valorDepois" 
                className="planeamento-input"
                value={formData.valorDepois} 
                onChange={handleChange} 
                required 
              />
            </div>

            <div className="form-group" style={{ gridColumn: 'span 2' }}>
              <label>Descrição do Ajuste:</label>
              <input 
                type="text" 
                name="descricao" 
                className="planeamento-input"
                value={formData.descricao} 
                onChange={handleChange} 
                placeholder="Ex: Reduzir frequência Linha 13 para fora de ponta"
              />
            </div>

            <div className="form-group">
              <label>Período Aplicável:</label>
              <select
                name="periodo"
                className="planeamento-select"
                value={formData.periodo}
                onChange={handleChange}
              >
                <option value="DIA_COMPLETO">Dia Completo</option>
                <option value="PONTA">Período de Ponta</option>
                <option value="FORA_PONTA">Fora de Ponta</option>
              </select>
            </div>

            <div style={{ gridColumn: 'span 3', display: 'flex', justifyContent: 'flex-end', borderTop: '1px solid #f1f5f9', paddingTop: '1rem', marginTop: '1rem' }}>
              <button type="submit" className="btn-primary" disabled={loadingSim}>
                {loadingSim ? 'A Executar Projeções...' : 'Simular e Estimar Impacto'}
              </button>
            </div>
          </form>

          {/* FA3: Error Message */}
          {errorMessage && (
            <div style={{ padding: '1rem', backgroundColor: '#fee2e2', color: '#b91c1c', borderRadius: '8px', border: '1px solid #fecaca', marginTop: '1rem' }}>
              <strong>Erro de Validação (FA3):</strong> {errorMessage}
            </div>
          )}

          {/* Painel de Projeção após simulação */}
          {projecao && (
            <div className="projecao-panel">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
                <h3 style={{ margin: 0, color: '#1e3a8a' }}>Resultado das Projeções Simuladas</h3>
                <span style={{ fontSize: '0.85rem', color: '#64748b' }}>
                  Versão dos Dados: <strong>{projecao.versaoDadosBase}</strong> (SHA: <code>{projecao.hashDadosBase?.substring(0, 8)}</code>)
                </span>
              </div>

              {/* FA1: Confiança Baixa Alert */}
              {projecao.avisoRisco && (
                <div style={{ padding: '0.75rem 1rem', backgroundColor: '#fffbeb', color: '#b45309', borderRadius: '8px', border: '1px solid #fef3c7', marginBottom: '1.25rem', fontSize: '0.9rem', fontWeight: 600 }}>
                  ⚠️ Alerta (FA1 - Histórico Insuficiente): Confiança estimada em apenas {projecao.nivelConfianca}%. Recomenda-se cautela ou aguardar mais dados históricos.
                </div>
              )}

              <div className="projecao-grid">
                <div className="projecao-card">
                  <span className="projecao-card-label">Ocupação Atual</span>
                  <p className="projecao-card-value">{projecao.ocupacaoAntes} pass/viagem</p>
                </div>
                <div className="projecao-card">
                  <span className="projecao-card-label">Ocupação Estimada</span>
                  <p className="projecao-card-value" style={{ color: '#2563eb' }}>{projecao.ocupacaoEsperada} pass/viagem</p>
                </div>
                <div className="projecao-card">
                  <span className="projecao-card-label">Perda de Receita</span>
                  <p className="projecao-card-value" style={{ color: '#dc2626' }}>-{projecao.receitaImpacto} €</p>
                </div>
                <div className="projecao-card">
                  <span className="projecao-card-label">Transferidos Alt.</span>
                  <p className="projecao-card-value" style={{ color: '#16a34a' }}>+{projecao.transferidosAlternativo} pass.</p>
                </div>
                <div className="projecao-card">
                  <span className="projecao-card-label">Confiança</span>
                  <p className="projecao-card-value" style={{ color: projecao.nivelConfianca >= 70 ? '#16a34a' : '#d97706' }}>
                    {projecao.nivelConfianca}%
                  </p>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Lista de Cenários Guardados */}
      <div className="planeamento-card" style={{ marginTop: '0.85rem' }}>
        <div className="planeamento-card-header">
          <h2>Cenários de Ajuste Guardados</h2>
        </div>
        <div className="planeamento-card-body">
          {loadingList ? (
            <div className="planeamento-empty-state">Carregando cenários...</div>
          ) : cenarios.length === 0 ? (
            <div className="planeamento-empty-state">Sem cenários registados.</div>
          ) : (
            <div className="planeamento-table-container">
              <table className="planeamento-table">
                <thead>
                  <tr>
                    <th style={{ textAlign: 'left' }}>Código</th>
                    <th style={{ textAlign: 'left' }}>Linha</th>
                    <th style={{ textAlign: 'left' }}>Descrição</th>
                    <th style={{ textAlign: 'right' }}>Ocupação Est.</th>
                    <th style={{ textAlign: 'right' }}>Perda Receita</th>
                    <th style={{ textAlign: 'center' }}>Confiança</th>
                    <th style={{ textAlign: 'center' }}>Estado</th>
                    <th style={{ textAlign: 'center' }}>Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {cenarios.map((row, idx) => (
                    <tr key={idx}>
                      <td style={{ fontWeight: 'bold', fontSize: '0.8rem' }}>{row.codigoCenario}</td>
                      <td>Linha {row.routeId}</td>
                      <td style={{ color: '#475569' }}>{row.descricao}</td>
                      <td style={{ textAlign: 'right' }}>{row.ocupacaoEsperada} pass/viag.</td>
                      <td style={{ textAlign: 'right', color: '#dc2626' }}>-{parseFloat(row.receitaImpacto || 0).toFixed(2)} €</td>
                      <td style={{ textAlign: 'center' }}>{row.nivelConfianca}%</td>
                      <td style={{ textAlign: 'center' }}>
                        <span className={getStatusBadgeClass(row.estado)}>{row.estado}</span>
                      </td>
                      <td style={{ textAlign: 'center' }}>
                        {row.estado === 'RASCUNHO' && (
                          <div style={{ display: 'flex', gap: '6px', justifyContent: 'center' }}>
                            <button 
                              onClick={() => handleUpdateStatus(row.codigoCenario, 'APPROVED')}
                              className="btn-aprovar"
                            >
                              Aprovar
                            </button>
                            <button 
                              onClick={() => handleUpdateStatus(row.codigoCenario, 'REJECTED')}
                              className="btn-rejeitar"
                            >
                              Rejeitar
                            </button>
                          </div>
                        )}
                        {row.estado === 'APPROVED' && (
                          <button 
                            onClick={() => handleUpdateStatus(row.codigoCenario, 'IMPLEMENTED')}
                            className="btn-implementar"
                          >
                            Implementar
                          </button>
                        )}
                        {row.estado === 'IMPLEMENTED' && (
                          <span style={{ fontSize: '0.75rem', color: '#64748b', fontWeight: 'bold' }}>Concluído</span>
                        )}
                        {row.estado === 'REJECTED' && (
                          <span style={{ fontSize: '0.75rem', color: '#94a3b8' }}>Arquivado</span>
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
    </div>
  );
}

export default SimulacaoCenarios;