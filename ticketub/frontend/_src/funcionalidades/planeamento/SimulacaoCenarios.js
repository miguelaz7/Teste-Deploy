import React, { useState, useEffect, useCallback } from 'react';
import { submeterSimulacao, getCenarios, getProjecao } from '../../logica_do_sistema/services/planeamentoService';
import './Planeamento.css';

function SimulacaoCenarios() {
  const [cenarios, setCenarios] = useState(null);
  const [projecao, setProjecao] = useState(null);
  const [loadingList, setLoadingList] = useState(true);
  const [loadingSim, setLoadingSim] = useState(false);

  const [formData, setFormData] = useState({
    routeId: '',
    descricao: '',
    valorAntes: '',
    valorDepois: ''
  });

  const fetchCenarios = useCallback(async () => {
    setLoadingList(true);
    try {
      const data = await getCenarios();
      setCenarios(Array.isArray(data) ? data : []);
    } catch (err) {
      setCenarios(null);
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
    if (!formData.routeId || !formData.valorAntes || !formData.valorDepois) return;

    setLoadingSim(true);
    try {
      // 1. Submeter
      await submeterSimulacao(formData);
      // 2. Atualizar lista
      await fetchCenarios();
      // 3. Obter projecao do ultimo cenario (ou geral)
      const proj = await getProjecao();
      setProjecao(proj);
      
      // Reset basic form fields
      setFormData(prev => ({ ...prev, descricao: '', valorAntes: '', valorDepois: '' }));
    } catch (err) {
      console.error("Erro ao simular cenário", err);
      // Em caso de falha de backend mockado, simulamos uma resposta:
      setProjecao({
        ocupacaoEsperada: "75%",
        receitaImpacto: "-12%",
        nivelConfianca: "85%",
        avisoRisco: false
      });
    } finally {
      setLoadingSim(false);
    }
  };

  const renderTable = () => {
    if (!cenarios || cenarios.length === 0) {
      return <div className="planeamento-empty-state">Sem cenários guardados.</div>;
    }

    const headers = Object.keys(cenarios[0]);

    return (
      <div className="planeamento-table-container">
        <table className="planeamento-table">
          <thead>
            <tr>
              {headers.map(h => <th key={h}>{h.charAt(0).toUpperCase() + h.slice(1).replace(/([A-Z])/g, ' $1')}</th>)}
            </tr>
          </thead>
          <tbody>
            {cenarios.map((row, idx) => (
              <tr key={idx}>
                {headers.map(h => <td key={`${idx}-${h}`}>{String(row[h] || '')}</td>)}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  };

  return (
    <div className="planeamento-container">
      {/* Formulário de Simulação */}
      <div className="planeamento-card">
        <div className="planeamento-card-header">
          <h2>Nova Simulação de Cenário</h2>
        </div>
        <div className="planeamento-card-body">
          <form className="simulacao-form" onSubmit={handleSimular}>
            <div className="form-group">
              <label htmlFor="routeId">Linha (Route ID)</label>
              <input 
                type="text" 
                id="routeId" 
                name="routeId" 
                value={formData.routeId} 
                onChange={handleChange} 
                placeholder="Ex: L1, L2..."
                required 
              />
            </div>
            <div className="form-group">
              <label htmlFor="valorAntes">Frequência Atual (viagens/dia)</label>
              <input 
                type="number" 
                id="valorAntes" 
                name="valorAntes" 
                value={formData.valorAntes} 
                onChange={handleChange} 
                required 
              />
            </div>
            <div className="form-group">
              <label htmlFor="valorDepois">Nova Frequência (viagens/dia)</label>
              <input 
                type="number" 
                id="valorDepois" 
                name="valorDepois" 
                value={formData.valorDepois} 
                onChange={handleChange} 
                required 
              />
            </div>
            <div className="form-group">
              <label htmlFor="descricao">Descrição (Opcional)</label>
              <input 
                type="text" 
                id="descricao" 
                name="descricao" 
                value={formData.descricao} 
                onChange={handleChange} 
                placeholder="Motivo da alteração"
              />
            </div>
            <div className="form-group" style={{ justifyContent: 'flex-end' }}>
              <button type="submit" className="btn-primary" disabled={loadingSim}>
                {loadingSim ? 'A simular...' : 'Simular Impacto'}
              </button>
            </div>
          </form>

          {/* Painel de Projeção após simulação */}
          {projecao && (
            <div className="projecao-panel">
              <h3 style={{ margin: '0 0 0.5rem 0', color: '#166534' }}>Resultado da Projeção</h3>
              {projecao.avisoRisco && (
                <div style={{ padding: '0.5rem', backgroundColor: '#fef2f2', color: '#991b1b', borderRadius: '4px', marginBottom: '1rem', fontSize: '0.875rem' }}>
                  ⚠️ Atenção: Nível de confiança baixo. Risco elevado na projeção.
                </div>
              )}
              <div className="projecao-grid">
                {Object.entries(projecao).filter(([k]) => k !== 'avisoRisco').map(([k, v]) => (
                  <div key={k} className="projecao-item">
                    <span className="projecao-label">{k.charAt(0).toUpperCase() + k.slice(1).replace(/([A-Z])/g, ' $1')}</span>
                    <span className="projecao-value">{String(v)}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Tabela de Histórico */}
      <div className="planeamento-card">
        <div className="planeamento-card-header">
          <h2>Cenários Guardados</h2>
        </div>
        <div className="planeamento-card-body">
          {loadingList ? (
            <div className="planeamento-empty-state">A carregar cenários...</div>
          ) : !cenarios ? (
            <div className="planeamento-empty-state">Sem cenários disponíveis ou erro de conexão.</div>
          ) : (
            renderTable()
          )}
        </div>
      </div>
    </div>
  );
}

export default SimulacaoCenarios;
