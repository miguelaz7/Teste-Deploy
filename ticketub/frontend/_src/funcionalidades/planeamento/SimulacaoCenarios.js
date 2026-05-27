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
    if (s === 'IMPLEMENTED') return 'badge-estado' /* custom blue */;
    return 'badge-estado estado-pendente';
  };

  return (
    <div className="planeamento-container">
      {/* Formulário de Simulação */}
      <div className="planeamento-card">
        <div className="planeamento-card-header">
          <h2>
            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2" style={{ marginRight: '6px' }}>
              <path d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            Nova Simulação de Cenário (UC11.1)
          </h2>
        </div>
        <div className="planeamento-card-body">
          <form className="simulacao-form" onSubmit={handleSimular} style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '1.25rem' }}>
            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: '#475569', marginBottom: '6px' }}>
                Linha (Route ID):
              </label>
              <input 
                type="text" 
                name="routeId" 
                style={{ width: '100%', padding: '0.5rem 0.8rem', border: '1px solid #cbd5e1', borderRadius: '8px' }}
                value={formData.routeId} 
                onChange={handleChange} 
                placeholder="Ex: 13, 42, 95"
                required 
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: '#475569', marginBottom: '6px' }}>
                Frequência Atual (circulações/hora):
              </label>
              <input 
                type="number" 
                name="valorAntes" 
                style={{ width: '100%', padding: '0.5rem 0.8rem', border: '1px solid #cbd5e1', borderRadius: '8px' }}
                value={formData.valorAntes} 
                onChange={handleChange} 
                required 
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: '#475569', marginBottom: '6px' }}>
                Frequência Projetada (circulações/hora):
              </label>
              <input 
                type="number" 
                name="valorDepois" 
                style={{ width: '100%', padding: '0.5rem 0.8rem', border: '1px solid #cbd5e1', borderRadius: '8px' }}
                value={formData.valorDepois} 
                onChange={handleChange} 
                required 
              />
            </div>

            <div style={{ gridColumn: 'span 2' }}>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: '#475569', marginBottom: '6px' }}>
                Descrição do Ajuste:
              </label>
              <input 
                type="text" 
                name="descricao" 
                style={{ width: '100%', padding: '0.5rem 0.8rem', border: '1px solid #cbd5e1', borderRadius: '8px' }}
                value={formData.descricao} 
                onChange={handleChange} 
                placeholder="Ex: Reduzir frequência Linha 13 para fora de ponta"
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: '#475569', marginBottom: '6px' }}>
                Período Aplicável:
              </label>
              <select
                name="periodo"
                style={{ width: '100%', padding: '0.5rem 0.8rem', border: '1px solid #cbd5e1', borderRadius: '8px', backgroundColor: '#fff' }}
                value={formData.periodo}
                onChange={handleChange}
              >
                <option value="DIA_COMPLETO">Dia Completo</option>
                <option value="PONTA">Período de Ponta</option>
                <option value="FORA_PONTA">Fora de Ponta</option>
              </select>
            </div>

            <div style={{ gridColumn: 'span 3', display: 'flex', justifyContent: 'flex-end', borderTop: '1px solid #f1f5f9', paddingTop: '1rem' }}>
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
            <div className="projecao-panel" style={{ marginTop: '1.5rem', padding: '1.5rem', background: '#f8fafc', borderRadius: '12px', border: '1px solid #e2e8f0' }}>
              
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

              <div className="projecao-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(5, 1fr)', gap: '1rem' }}>
                <div style={{ background: '#fff', padding: '1rem', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
                  <span style={{ fontSize: '0.75rem', color: '#64748b', fontWeight: 'bold' }}>OCUPAÇÃO ATUAL</span>
                  <p style={{ margin: '0.25rem 0 0 0', fontSize: '1.4rem', fontWeight: 800 }}>{projecao.ocupacaoAntes} pass/viagem</p>
                </div>
                <div style={{ background: '#fff', padding: '1rem', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
                  <span style={{ fontSize: '0.75rem', color: '#64748b', fontWeight: 'bold' }}>OCUPAÇÃO ESTIMADA</span>
                  <p style={{ margin: '0.25rem 0 0 0', fontSize: '1.4rem', fontWeight: 800, color: '#2563eb' }}>{projecao.ocupacaoEsperada} pass/viagem</p>
                </div>
                <div style={{ background: '#fff', padding: '1rem', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
                  <span style={{ fontSize: '0.75rem', color: '#64748b', fontWeight: 'bold' }}>PERDA DE RECEITA</span>
                  <p style={{ margin: '0.25rem 0 0 0', fontSize: '1.4rem', fontWeight: 800, color: '#dc2626' }}>-{projecao.receitaImpacto} €</p>
                </div>
                <div style={{ background: '#fff', padding: '1rem', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
                  <span style={{ fontSize: '0.75rem', color: '#64748b', fontWeight: 'bold' }}>TRANSFERIDOS ALT.</span>
                  <p style={{ margin: '0.25rem 0 0 0', fontSize: '1.4rem', fontWeight: 800, color: '#16a34a' }}>+{projecao.transferidosAlternativo} pass.</p>
                </div>
                <div style={{ background: '#fff', padding: '1rem', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
                  <span style={{ fontSize: '0.75rem', color: '#64748b', fontWeight: 'bold' }}>CONFIANÇA</span>
                  <p style={{ margin: '0.25rem 0 0 0', fontSize: '1.4rem', fontWeight: 800, color: projecao.nivelConfianca >= 70 ? '#16a34a' : '#d97706' }}>
                    {projecao.nivelConfianca} %
                  </p>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Lista de Cenários Guardados */}
      <div className="planeamento-card" style={{ marginTop: '1.5rem' }}>
        <div className="planeamento-card-header">
          <h2>Cenários de Ajuste Guardados (UC11.3)</h2>
        </div>
        <div className="planeamento-card-body">
          {loadingList ? (
            <div className="planeamento-empty-state">Carregando cenários...</div>
          ) : cenarios.length === 0 ? (
            <div className="planeamento-empty-state">Sem cenários registados.</div>
          ) : (
            <div className="planeamento-table-container">
              <table className="planeamento-table" style={{ width: '100%', borderCollapse: 'collapse' }}>
                <thead>
                  <tr style={{ backgroundColor: '#f8fafc' }}>
                    <th style={{ padding: '10px', textAlign: 'left' }}>Código</th>
                    <th style={{ padding: '10px', textAlign: 'left' }}>Linha</th>
                    <th style={{ padding: '10px', textAlign: 'left' }}>Descrição</th>
                    <th style={{ padding: '10px', textAlign: 'right' }}>Ocupação Est.</th>
                    <th style={{ padding: '10px', textAlign: 'right' }}>Perda Receita</th>
                    <th style={{ padding: '10px', textAlign: 'center' }}>Confiança</th>
                    <th style={{ padding: '10px', textAlign: 'center' }}>Estado</th>
                    <th style={{ padding: '10px', textAlign: 'center' }}>Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {cenarios.map((row, idx) => (
                    <tr key={idx} style={{ borderBottom: '1px solid #f1f5f9' }}>
                      <td style={{ padding: '10px', fontWeight: 'bold', fontSize: '0.8rem' }}>{row.codigoCenario}</td>
                      <td style={{ padding: '10px' }}>Linha {row.routeId}</td>
                      <td style={{ padding: '10px', color: '#475569' }}>{row.descricao}</td>
                      <td style={{ padding: '10px', textAlign: 'right' }}>{row.ocupacaoEsperada} pass/viag.</td>
                      <td style={{ padding: '10px', textAlign: 'right', color: '#dc2626' }}>-{parseFloat(row.receitaImpacto || 0).toFixed(2)} €</td>
                      <td style={{ padding: '10px', textAlign: 'center' }}>{row.nivelConfianca}%</td>
                      <td style={{ padding: '10px', textAlign: 'center' }}>
                        <span className={getStatusBadgeClass(row.estado)}>{row.estado}</span>
                      </td>
                      <td style={{ padding: '10px', textAlign: 'center' }}>
                        {row.estado === 'RASCUNHO' && (
                          <div style={{ display: 'flex', gap: '6px', justifyContent: 'center' }}>
                            <button 
                              onClick={() => handleUpdateStatus(row.codigoCenario, 'APPROVED')}
                              style={{ padding: '2px 8px', background: '#16a34a', color: '#fff', border: 'none', borderRadius: '4px', cursor: 'pointer', fontSize: '0.75rem', fontWeight: 'bold' }}
                            >
                              Aprovar
                            </button>
                            <button 
                              onClick={() => handleUpdateStatus(row.codigoCenario, 'REJECTED')}
                              style={{ padding: '2px 8px', background: '#dc2626', color: '#fff', border: 'none', borderRadius: '4px', cursor: 'pointer', fontSize: '0.75rem', fontWeight: 'bold' }}
                            >
                              Rejeitar
                            </button>
                          </div>
                        )}
                        {row.estado === 'APPROVED' && (
                          <button 
                            onClick={() => handleUpdateStatus(row.codigoCenario, 'IMPLEMENTED')}
                            style={{ padding: '2px 8px', background: '#2563eb', color: '#fff', border: 'none', borderRadius: '4px', cursor: 'pointer', fontSize: '0.75rem', fontWeight: 'bold' }}
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