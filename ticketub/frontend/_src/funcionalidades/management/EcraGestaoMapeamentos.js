import React, { useState, useEffect, useCallback } from 'react';
import {
  getMappings,
  createMapping,
  updateMapping,
  deleteMapping,
  logAuditAction,
  reprocessEvents,
  resetEvents,
  getPendingNaoCategorizados,
  resolverNaoCategorizado
} from '../../logica_do_sistema/services/categorizationService';
import './EcraGestaoMapeamentos.css';

const EcraGestaoMapeamentos = ({ onMappingChange }) => {
  const [activeTab, setActiveTab] = useState('mapeamentos');
  const [mappings, setMappings] = useState([]);
  const [naoCategorizados, setNaoCategorizados] = useState([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [editingAnterior, setEditingAnterior] = useState(null);
  const [resolvingEventId, setResolvingEventId] = useState(null);
  const [formData, setFormData] = useState({ tipo_titulo: '', perfil: 'estudante' });

  const fetchMappings = useCallback(async (silencioso = false) => {
    try {
      const data = await getMappings();
      setMappings(Array.isArray(data) ? data : []);
    } catch (err) {
      if (!silencioso) console.error('Erro a carregar mapeamentos', err);
    }
  }, []);

  const fetchNaoCategorizados = useCallback(async () => {
    try {
      const data = await getPendingNaoCategorizados();
      setNaoCategorizados(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Erro ao carregar nao categorizados', err);
    }
  }, []);

  const loadData = useCallback(async (silencioso = false) => {
    if (activeTab === 'mapeamentos') {
      await fetchMappings(silencioso);
    } else if (activeTab === 'nao_categorizados') {
      await fetchNaoCategorizados();
    }
  }, [activeTab, fetchMappings, fetchNaoCategorizados]);

  useEffect(() => {
    loadData(false);
    const intervalId = setInterval(() => { loadData(true); }, 4000);
    return () => clearInterval(intervalId);
  }, [loadData]);

  const handleOpenModal = (mapping = null, pendingEvent = null) => {
    if (pendingEvent) {
      setFormData({ tipo_titulo: pendingEvent.tipoTitulo, perfil: 'estudante' });
      setEditingId(null);
      setEditingAnterior(null);
      setResolvingEventId(pendingEvent.id);
    } else if (mapping) {
      setFormData({ tipo_titulo: mapping.tipoTitulo, perfil: mapping.perfil });
      setEditingId(mapping.id);
      setEditingAnterior(mapping.perfil);
      setResolvingEventId(null);
    } else {
      setFormData({ tipo_titulo: 'MENSAL', perfil: 'estudante' });
      setEditingId(null);
      setEditingAnterior(null);
      setResolvingEventId(null);
    }
    setIsModalOpen(true);
  };

  const handleReset = async () => {
    if (window.confirm('TEM A CERTEZA? Isto vai apagar todos os mapeamentos e limpar todas as classificações da base de dados!')) {
      try {
        await resetEvents();
        await reprocessEvents();
        if (onMappingChange) onMappingChange();
        await fetchMappings(false);
        alert('Sistema reiniciado com sucesso.');
      } catch (err) {
        console.error('Erro ao reiniciar sistema', err);
      }
    }
  };

  const handleCloseModal = () => {
    setIsModalOpen(false);
    setEditingId(null);
    setEditingAnterior(null);
    setResolvingEventId(null);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (editingId) {
        await updateMapping(editingId, formData);
      } else {
        await createMapping(formData);
      }
    } catch (err) {
      console.error('Erro a guardar mapeamento', err);
      return;
    }

    try {
      await logAuditAction(
        editingId ? 'UPDATE' : 'CREATE',
        formData.tipo_titulo,
        editingAnterior,
        formData.perfil,
        'admin'
      );
    } catch {}

    if (resolvingEventId) {
      try {
        await resolverNaoCategorizado(resolvingEventId, 'RECLASSIFICADO', 'admin');
      } catch (err) {
        console.error('Erro ao resolver evento nao categorizado', err);
      }
    }

    try { await reprocessEvents(); } catch {}
    if (onMappingChange) onMappingChange();
    await loadData(false);
    handleCloseModal();
  };

  const handleDelete = async (id, tipoTitulo, perfil) => {
    if (window.confirm(`Tem a certeza que deseja apagar o mapeamento para "${tipoTitulo}"?`)) {
      try {
        await deleteMapping(id);
        await logAuditAction('DELETE', tipoTitulo, perfil, null, 'admin');
        await reprocessEvents();
        if (onMappingChange) onMappingChange();
        await fetchMappings(false);
      } catch (err) {
        console.error('Erro a apagar mapeamento', err);
      }
    }
  };

  const handleRejeitarEvento = async (id) => {
    if (window.confirm('Tem a certeza que deseja rejeitar definitivamente este evento sem tipologia?')) {
      try {
        await resolverNaoCategorizado(id, 'REJEITADO', 'admin');
        await fetchNaoCategorizados();
      } catch (err) {
        console.error('Erro ao rejeitar evento', err);
      }
    }
  };

  const handleReprocessar = async () => {
    try {
      await reprocessEvents();
      alert('Reprocessamento retroativo desencadeado com sucesso.');
      await loadData(false);
    } catch (err) {
      console.error('Erro ao reprocessar', err);
    }
  };

  return (
    <div className="gestao-mapeamentos-container">
      <div className="gestao-header">
        <h2>Gestão de Classificação Tarifária</h2>
      </div>

      <div className="management-tabs">
        <button
          className={`management-tab-btn ${activeTab === 'mapeamentos' ? 'active' : ''}`}
          onClick={() => setActiveTab('mapeamentos')}
        >
          Mapeamento de Perfis
        </button>
        <button
          className={`management-tab-btn ${activeTab === 'nao_categorizados' ? 'active' : ''}`}
          onClick={() => setActiveTab('nao_categorizados')}
        >
          Fila de Revisão ({naoCategorizados.length})
        </button>
      </div>

      {activeTab === 'mapeamentos' && (
        <>
          <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '1rem', gap: '0.75rem' }}>
            <button className="btn-secondary" onClick={handleReset}>
              Limpar Tudo
            </button>
            <button className="btn-primary" onClick={() => handleOpenModal()}>
              Adicionar Mapeamento
            </button>
          </div>

          <div className="table-container">
            <table className="mappings-table">
              <thead>
                <tr>
                  <th>Tipo de Título</th>
                  <th>Perfil Classificado</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                {mappings.length > 0 ? (
                  mappings.map((m) => (
                    <tr key={m.id}>
                      <td style={{ fontWeight: 700 }}>{m.tipoTitulo}</td>
                      <td>{m.perfil.charAt(0).toUpperCase() + m.perfil.slice(1)}</td>
                      <td>
                        <button className="btn-icon-edit" onClick={() => handleOpenModal(m)}>Editar</button>
                        <button className="btn-icon-delete" onClick={() => handleDelete(m.id, m.tipoTitulo, m.perfil)}>Apagar</button>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan="3" style={{ textAlign: 'center', color: '#64748b', padding: '2rem' }}>
                      Sem mapeamentos definidos.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </>
      )}

      {activeTab === 'nao_categorizados' && (
        <>
          <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '1rem' }}>
            <button className="btn-primary" onClick={handleReprocessar}>
              Forçar Reprocessamento Retroativo
            </button>
          </div>

          <div className="table-container">
            <table className="mappings-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Tipologia Desconhecida</th>
                  <th>Motivo Rejeição</th>
                  <th>Estado</th>
                  <th>Criado Em</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                {naoCategorizados.length > 0 ? (
                  naoCategorizados.map((nc) => (
                    <tr key={nc.id}>
                      <td>{nc.id}</td>
                      <td style={{ fontWeight: 700, color: '#dc2626' }}>{nc.tipoTitulo}</td>
                      <td>{nc.motivoRejeicao || 'Tipologia não mapeada'}</td>
                      <td>
                        <span className="badge-status pending">{nc.status}</span>
                      </td>
                      <td style={{ fontSize: '0.8rem', color: '#64748b' }}>
                        {nc.createdAt ? new Date(nc.createdAt).toLocaleString() : '—'}
                      </td>
                      <td>
                        <button
                          className="btn-icon-approve"
                          onClick={() => handleOpenModal(null, nc)}
                        >
                          Mapear e Corrigir
                        </button>
                        <button
                          className="btn-icon-delete"
                          onClick={() => handleRejeitarEvento(nc.id)}
                        >
                          Rejeitar
                        </button>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan="6" style={{ textAlign: 'center', color: '#059669', padding: '3rem', fontWeight: 600 }}>
                      ✓ Excelente! Fila de revisão vazia. Todos os títulos estão corretamente categorizados.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </>
      )}

      {isModalOpen && (
        <div className="modal-overlay" style={{ zIndex: 99999, position: 'fixed' }}>
          <div className="modal-content">
            <div className="modal-header">
              {editingId ? 'Editar Mapeamento' : resolvingEventId ? 'Mapear Tipologia Desconhecida' : 'Novo Mapeamento'}
            </div>
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label>Tipo de Título</label>
                {resolvingEventId ? (
                  <input
                    type="text"
                    value={formData.tipo_titulo}
                    readOnly
                    style={{ backgroundColor: '#f1f5f9', cursor: 'not-allowed', fontWeight: 700 }}
                  />
                ) : (
                  <input
                    type="text"
                    value={formData.tipo_titulo}
                    onChange={(e) => setFormData({ ...formData, tipo_titulo: e.target.value.toUpperCase() })}
                    placeholder="Ex: PASSE_ESTUDANTE_SCB"
                    required
                  />
                )}
              </div>
              <div className="form-group">
                <label>Perfil Classificado</label>
                <select
                  value={formData.perfil}
                  onChange={(e) => setFormData({ ...formData, perfil: e.target.value })}
                >
                  <option value="estudante">Estudante</option>
                  <option value="senior">Sénior</option>
                  <option value="normal">Normal</option>
                  <option value="nao_categorizado">Não Categorizado</option>
                </select>
              </div>
              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={handleCloseModal}>Cancelar</button>
                <button type="submit" className="btn-primary">Guardar e Aplicar</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default EcraGestaoMapeamentos;